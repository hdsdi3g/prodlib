package tv.hd3g.jobkit.engine;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobKitEngine implements JobTrait {

	private final List<BackgroundService> backgroundServices;
	private final ScheduledExecutorService scheduledExecutor;
	private final BackgroundServiceEvent backgroundServiceEvent;
	private final Spooler spooler;
	private final SupervisableManager supervisableManager;
	private final AtomicBoolean shutdown;
	private final Set<String> spoolsNamesToKeepRunningToTheEnd;

	public JobKitEngine(final ScheduledExecutorService scheduledExecutor,
						final ExecutionEvent executionEvent,
						final BackgroundServiceEvent backgroundServiceEvent,
						final SupervisableManager supervisableManager) {
		this.scheduledExecutor = scheduledExecutor;
		this.backgroundServiceEvent = backgroundServiceEvent;
		backgroundServices = Collections.synchronizedList(new ArrayList<>());
		spoolsNamesToKeepRunningToTheEnd = Collections.synchronizedSet(new HashSet<>());
		this.supervisableManager = supervisableManager;
		shutdown = new AtomicBoolean(false);

		if (supervisableManager == null) {
			spooler = new Spooler(executionEvent, SupervisableManager.voidSupervisableEvents());
		} else {
			spooler = new Spooler(executionEvent, supervisableManager);
		}

		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
	}

	public JobKitEngine(final ScheduledExecutorService scheduledExecutor,
						final ExecutionEvent executionEvent,
						final BackgroundServiceEvent backgroundServiceEvent) {
		this(scheduledExecutor, executionEvent, backgroundServiceEvent, null);
	}

	protected JobKitEngine() {
		scheduledExecutor = null;
		backgroundServiceEvent = null;
		spooler = null;
		backgroundServices = null;
		supervisableManager = null;
		shutdown = new AtomicBoolean(false);
		spoolsNamesToKeepRunningToTheEnd = null;
	}

	private void checkNoShutdown() {
		if (shutdown.get()) {
			throw new IllegalStateException("JobKit and app is currently to close. Can't add new jobs.");
		}
	}

	/**
	 * @return true if the task is queued
	 */
	@Override
	public boolean runOneShot(final String name,
							  final String spoolName,
							  final int priority,
							  final RunnableWithException task,
							  final Consumer<Exception> afterRunCommand) {
		checkNoShutdown();
		return spooler.getExecutor(spoolName).addToQueue(task, name, priority, afterRunCommand);
	}

	public BackgroundService createService(final String name, // NOSONAR S1133
										   final String spoolName,
										   final RunnableWithException serviceTask,
										   final RunnableWithException onServiceDisableTask) {
		checkNoShutdown();
		final var service = new BackgroundService(name,
				spoolName,
				spooler,
				scheduledExecutor,
				backgroundServiceEvent,
				serviceTask,
				onServiceDisableTask);
		backgroundServices.add(service);
		return service;
	}

	/**
	 * Create a service if not exists as "name".
	 */
	public BackgroundService startService(final String name, // NOSONAR S1133
										  final String spoolName,
										  final long timedInterval,
										  final TimeUnit unit,
										  final RunnableWithException serviceTask,
										  final RunnableWithException onServiceDisableTask) {
		checkNoShutdown();
		return createService(name, spoolName, serviceTask, onServiceDisableTask)
				.setTimedInterval(timedInterval, unit)
				.enable();
	}

	/**
	 * Create a service if not exists as "name".
	 */
	public BackgroundService startService(final String name,
										  final String spoolName,
										  final Duration duration,
										  final RunnableWithException serviceTask,
										  final RunnableWithException onServiceDisableTask) {
		checkNoShutdown();
		return startService(name, spoolName, duration.toMillis(), MILLISECONDS, serviceTask, onServiceDisableTask);
	}

	public Spooler getSpooler() {
		return spooler;
	}

	public void onApplicationReadyRunBackgroundServices() {
		checkNoShutdown();
		backgroundServices.forEach(BackgroundService::runFirstOnStartup);
	}

	public Set<String> getSpoolsNamesToKeepRunningToTheEnd() {
		return spoolsNamesToKeepRunningToTheEnd;
	}

	/**
	 * This should never be called outside ShutdownHook. Only visible here for test purpose.
	 */
	void shutdown() {
		log.warn("App want to close: shutdown jobKitEngine...");
		shutdown.set(true);
		backgroundServices.forEach(BackgroundService::disable);
		scheduledExecutor.shutdown();
		spooler.shutdown(spoolsNamesToKeepRunningToTheEnd);
		Optional.ofNullable(supervisableManager).ifPresent(SupervisableManager::close);
	}

	private class ShutdownHook extends Thread {

		public ShutdownHook() {
			setName("JobKit ShutdownHook");
			setPriority(Thread.MAX_PRIORITY);
			setDaemon(true);
		}

		@Override
		public void run() {
			if (shutdown.get()) {
				log.warn("Why on God do you have run shutdown() before here?");
				return;
			}
			shutdown();
			log.warn("JobKitEngine is now closed properly");
		}

	}

}
