package tv.hd3g.jobkit.engine;

import static java.util.function.Predicate.not;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpoolExecutor {

	@Getter
	private final String name;
	private final ExecutionEvent event;
	private final AtomicLong threadCount;

	private final AtomicComputeReference<Thread> currentOperation;
	private final Comparator<SpoolJob> queueComparator;
	private final PriorityBlockingQueue<SpoolJob> queue;
	private final AtomicBoolean shutdown;
	private final SupervisableEvents supervisableEvents;
	private final JobKitWatchdog jobKitWatchdog;

	SpoolExecutor(final String name,
				  final ExecutionEvent event,
				  final AtomicLong threadCount,
				  final SupervisableEvents supervisableEvents,
				  final JobKitWatchdog jobKitWatchdog) {
		this.name = name;
		this.event = event;
		this.threadCount = threadCount;
		this.jobKitWatchdog = jobKitWatchdog;
		queueComparator = (l, r) -> {
			final var compared = Integer.compare(r.jobPriority, l.jobPriority);
			if (compared == 0) {
				return Long.compare(l.createdIndex, r.createdIndex);
			}
			return compared;
		};
		queue = new PriorityBlockingQueue<>(1, queueComparator);
		shutdown = new AtomicBoolean(false);
		this.supervisableEvents = supervisableEvents;
		currentOperation = new AtomicComputeReference<>();
	}

	boolean addToQueue(final RunnableWithException command,
					   final String name,
					   final int priority,
					   final Consumer<Exception> afterRunCommand) {
		if (shutdown.get()) {
			log.error("Can't add to queue new command \"{}\" by \"{}\": the spool is shutdown", name, this.name);
			return false;
		}
		final var newJob = new SpoolJob(command, name, priority, afterRunCommand, this);
		queue.add(newJob);
		jobKitWatchdog.addJob(newJob);
		log.debug("Add new command \"{}\" by \"{}\" with P{}", name, this.name, priority);
		runNext();
		return true;
	}

	int getQueueSize() {
		return queue.size();
	}

	boolean isRunning() {
		return currentOperation.computePredicate(Thread::isAlive);
	}

	/**
	 * @param executor never put him in this queue's jobs!
	 * @return blocker
	 *         Infinit blocking if some jobs create new jobs in the same queue...
	 */
	public Future<Void> waitToEndQueue(final Executor executor) {
		return CompletableFuture.runAsync(() -> {
			while (queue.isEmpty() == false
				   || currentOperation.isSet()) {
				Thread.onSpinWait();
			}
		}, executor);
	}

	private void runNext() {
		if (shutdown.get()) {
			/**
			 * shutdown() will keep run the last jobs on queue
			 */
			return;
		}
		synchronized (queue) {
			if (isRunning()) {
				return;
			}
			final var next = queue.poll();
			if (next == null) {
				currentOperation.reset();
				return;
			}
			currentOperation.setAnd(next, Thread::start);
		}
	}

	void stopToAcceptNewJobs() {
		log.debug("Stop spool {} to accept new jobs", name);
		shutdown.set(true);
	}

	/**
	 * Blocking
	 */
	void clean(final boolean purgeWaitList) {
		log.debug("Clean spool {}", name);

		if (purgeWaitList) {
			queue.clear();
		}
		while (isRunning()) {
			Thread.onSpinWait();
		}

		if (purgeWaitList) {
			log.debug("Spool {} is cleaned (with {} canceled task(s))", name, queue.size());
			return;
		}
		if (queue.isEmpty() == false) {
			log.debug("Wait {} jobs to run before clean spool {}...", name, queue.size());

			Thread endOperation;
			endOperation = queue.poll();
			while (endOperation != null) {
				endOperation.start();
				while (endOperation.isAlive()) {
					Thread.onSpinWait();
				}
				endOperation = queue.poll();
			}
		}
		log.debug("Spool {} is now empty, without running tasks", name);
	}

	private static Optional<StackTraceElement> getCaller() {
		return Stream.of(new Throwable().getStackTrace())
				.filter(not(StackTraceElement::isNativeMethod))
				.filter(not(t -> t.getClassName().startsWith(SpoolExecutor.class.getPackageName())))
				.findFirst();
	}

	private class SpoolJob extends Thread implements SupervisableSupplier, WatchableSpoolJob {

		final RunnableWithException command;
		@Getter
		final String commandName;
		final int jobPriority;
		final Consumer<Exception> afterRunCommand;
		@Getter
		final SpoolExecutor executorReferer;
		final AtomicReference<Supervisable> supervisableReference;
		@Getter
		final long createdIndex;
		@Getter
		final Optional<StackTraceElement> creator;

		SpoolJob(final RunnableWithException command,
				 final String commandName,
				 final int jobPriority,
				 final Consumer<Exception> afterRunCommand,
				 final SpoolExecutor executorReferer) {
			super();
			createdIndex = threadCount.getAndIncrement();
			setName("SpoolExecutor #" + createdIndex);
			setPriority(MIN_PRIORITY);
			setDaemon(false);

			this.command = command;
			this.commandName = commandName;
			this.jobPriority = jobPriority;
			this.afterRunCommand = afterRunCommand;
			this.executorReferer = executorReferer;
			supervisableReference = new AtomicReference<>();
			creator = getCaller();
		}

		private Supervisable createSupervisable(final String jobName) {
			final var s = new Supervisable(name, jobName, supervisableEvents);
			supervisableReference.set(s);
			return s;
		}

		@Override
		public void run() {
			final var startTime = System.currentTimeMillis();
			jobKitWatchdog.startJob(this, startTime);

			var currentSupervisable = createSupervisable(commandName + " beforeRunJob");
			try {
				supervisableReference.set(currentSupervisable);
				currentSupervisable.start();
				event.beforeStart(commandName, System.currentTimeMillis(), executorReferer);
				currentSupervisable.end();
			} catch (final Exception e) {
				log.warn("Can't send event BeforeStart", e);
				currentSupervisable.end(e);
			}

			currentSupervisable = createSupervisable(commandName);
			Exception error = null;
			try {
				log.debug("Start new command \"{}\" by \"{}\"", commandName, name);
				currentSupervisable.start();
				command.run();
				currentSupervisable.end();
				log.debug("Ends correcly command \"{}\" by \"{}\", after {} sec", commandName, name,
						(System.currentTimeMillis() - startTime) / 1000f);
			} catch (final Exception e) {
				error = e;
				log.warn("Command \"{}\" by \"{}\", failed after {} sec", commandName, name,
						(System.currentTimeMillis() - startTime) / 1000f, e);
				currentSupervisable.end(e);
			}

			final var endTime = System.currentTimeMillis();
			currentSupervisable = createSupervisable(commandName + " afterRunJob");
			try {
				currentSupervisable.start();
				if (error != null) {
					event.afterFailedRun(commandName, endTime, endTime - startTime, executorReferer, error);
				} else {
					event.afterRunCorrectly(commandName, endTime, endTime - startTime, executorReferer);
				}
				currentSupervisable.end();
			} catch (final Exception e) {
				currentSupervisable.end(e);
				log.warn("Can't send event afterRun", e);
			}

			currentSupervisable = createSupervisable(commandName + " endsJob");
			try {
				final var startTimeAfterRun = System.currentTimeMillis();
				log.debug("Start to run afterRunCommand for  \"{}\" by \"{}\"", commandName, name);
				currentSupervisable.start();
				afterRunCommand.accept(error);
				currentSupervisable.end();
				log.debug("Ends correcly afterRunCommand \"{}\" by \"{}\", after {} sec", commandName, name,
						(System.currentTimeMillis() - startTimeAfterRun) / 1000f);
			} catch (final Exception e) {
				currentSupervisable.end(e);
				log.error("Fail to run afterRunCommand for  \"{}\" by \"{}\"", commandName, name, e);
			}

			supervisableReference.set(null);

			jobKitWatchdog.endJob(this);

			synchronized (queue) {
				currentOperation.reset();
			}
			runNext();
		}

		@Override
		public Supervisable getSupervisable() {
			return Optional.ofNullable(supervisableReference.get())
					.orElseThrow(() -> new IllegalThreadStateException(
							"Thread don't expose now a Supervisable: it's not run."));
		}

	}

}
