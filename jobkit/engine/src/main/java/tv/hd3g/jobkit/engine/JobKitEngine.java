package tv.hd3g.jobkit.engine;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class JobKitEngine implements JobTrait {

	private final List<BackgroundService> backgroundServices;
	private final ScheduledExecutorService scheduledExecutor;
	private final BackgroundServiceEvent backgroundServiceEvent;
	private final Spooler spooler;
	private final SupervisableManager supervisableManager;

	public JobKitEngine(final ScheduledExecutorService scheduledExecutor,
						final ExecutionEvent executionEvent,
						final BackgroundServiceEvent backgroundServiceEvent,
						final SupervisableManager supervisableManager) {
		this.scheduledExecutor = scheduledExecutor;
		this.backgroundServiceEvent = backgroundServiceEvent;
		backgroundServices = Collections.synchronizedList(new ArrayList<>());
		this.supervisableManager = supervisableManager;

		if (supervisableManager == null) {
			spooler = new Spooler(executionEvent, SupervisableManager.voidSupervisableEvents());
		} else {
			spooler = new Spooler(executionEvent, supervisableManager.getLifeCycle());
		}
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
		return spooler.getExecutor(spoolName).addToQueue(task, name, priority, afterRunCommand);
	}

	/**
	 * @return a new service or the existing service for "name"
	 */
	public BackgroundService createService(final String name,
										   final String spoolName,
										   final RunnableWithException task) {
		final var service = new BackgroundService(name,
				spoolName,
				spooler,
				scheduledExecutor,
				backgroundServiceEvent,
				task);
		backgroundServices.add(service);
		return service;
	}

	/**
	 * Create a service if not exists as "name".
	 */
	public BackgroundService startService(final String name,
										  final String spoolName,
										  final long timedInterval,
										  final TimeUnit unit,
										  final RunnableWithException task) {
		return createService(name, spoolName, task)
				.setTimedInterval(timedInterval, unit)
				.enable();
	}

	/**
	 * Create a service if not exists as "name".
	 */
	public BackgroundService startService(final String name,
										  final String spoolName,
										  final Duration duration,
										  final RunnableWithException task) {
		return startService(name, spoolName, duration.toMillis(), MILLISECONDS, task);
	}

	public Spooler getSpooler() {
		return spooler;
	}

	/**
	 * Stop all services and shutdown spooler.
	 * Non-blocking.
	 * Don't forget to shutdown the scheduled executor.
	 */
	public void shutdown() {
		backgroundServices.forEach(BackgroundService::disable);
		spooler.shutdown();
		Optional.ofNullable(supervisableManager).ifPresent(SupervisableManager::close);
	}

	/**
	 * Blocking. It call shutdown() before.
	 */
	public void waitToClose() {
		shutdown();
		spooler.waitToClose();
	}

	public void onApplicationReadyRunBackgroundServices() {
		backgroundServices.forEach(BackgroundService::runFirstOnStartup);
	}

}
