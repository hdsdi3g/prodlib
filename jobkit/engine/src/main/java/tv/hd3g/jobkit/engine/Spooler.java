package tv.hd3g.jobkit.engine;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Spooler {

	private final ConcurrentHashMap<String, SpoolExecutor> spoolExecutors;
	private final ExecutionEvent event;
	private final AtomicLong threadCount;
	private final AtomicBoolean shutdown;
	private final SupervisableEvents supervisableEvents;
	@Getter
	private final JobKitWatchdog jobKitWatchdog;

	public Spooler(final ExecutionEvent event,
				   final SupervisableEvents supervisableEvents,
				   final JobKitWatchdog jobKitWatchdog) {
		this.event = event;
		spoolExecutors = new ConcurrentHashMap<>();
		threadCount = new AtomicLong(0);
		shutdown = new AtomicBoolean(false);
		this.supervisableEvents = supervisableEvents;
		this.jobKitWatchdog = jobKitWatchdog;
	}

	private Stream<SpoolExecutor> getSpoolExecutorStream() {
		return spoolExecutors.entrySet().stream().map(Entry::getValue);
	}

	/**
	 * @return actual or new Executor (it will be created as needed).
	 *         Can't create new if this spooler is shutdown (return null).
	 */
	public SpoolExecutor getExecutor(final String name) {
		if (shutdown.get()) {
			return spoolExecutors.get(name);
		}
		return spoolExecutors.computeIfAbsent(name,
				n -> new SpoolExecutor(n, event, threadCount, supervisableEvents, jobKitWatchdog));
	}

	public int getAllQueuesSize() {
		return getSpoolExecutorStream().mapToInt(SpoolExecutor::getQueueSize).sum();
	}

	public int getRunningQueuesCount() {
		return (int) getSpoolExecutorStream().filter(SpoolExecutor::isRunning).count();
	}

	/**
	 * Blocking
	 */
	public void shutdown(final Set<String> spoolsNamesToKeepRunningToTheEnd) {
		if (shutdown.get()) {
			return;
		}
		jobKitWatchdog.shutdown();
		shutdown.set(true);

		final var count = getRunningQueuesCount();
		log.info("Shutdown all ({}) spoolExecutors. {} are running jobs and {} in waiting...",
				spoolExecutors.mappingCount(),
				count,
				getAllQueuesSize());

		getSpoolExecutorStream().forEach(SpoolExecutor::stopToAcceptNewJobs);

		spoolExecutors.entrySet().stream()
				.filter(entry -> spoolsNamesToKeepRunningToTheEnd.contains(entry.getKey()) == false)
				.map(Entry::getValue)
				.forEach(se -> se.clean(true));

		spoolExecutors.entrySet().stream()
				.filter(entry -> spoolsNamesToKeepRunningToTheEnd.contains(entry.getKey()))
				.map(Entry::getValue)
				.forEach(se -> se.clean(false));

		final var s = new Supervisable(Thread.currentThread().toString(), "ShutdownSpooler", supervisableEvents);
		s.start();
		event.shutdownSpooler(s);
		s.end();
	}

}
