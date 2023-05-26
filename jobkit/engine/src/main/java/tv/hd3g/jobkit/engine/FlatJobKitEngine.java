/*
 * This file is part of jobkit-engine.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.jobkit.engine;

import static tv.hd3g.jobkit.engine.Supervisable.manuallyRegistedSupervisables;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FlatJobKitEngine extends JobKitEngine {
	private static final Logger log = LogManager.getLogger();
	private static final String AFTER = "AFTER ";

	private final FlatScheduledExecutorService flatShExecutor;
	private final List<SupervisableEndEvent> endEvents;
	private final FlatSupervisableEvents supervisableEvents;
	private final List<RunnableWithException> disableTaskList;

	public FlatJobKitEngine() {
		super();
		flatShExecutor = new FlatScheduledExecutorService();
		endEvents = Collections.synchronizedList(new ArrayList<>());
		disableTaskList = Collections.synchronizedList(new ArrayList<>());
		supervisableEvents = new FlatSupervisableEvents();
	}

	private class FlatSupervisableEvents implements SupervisableEvents {
		@Override
		public void onEnd(final Supervisable supervisable, final Optional<Exception> oError) {
			final var endEvent = supervisable.getEndEvent(oError, "flatManager");
			if (endEvent.isEmpty()) {
				log.trace("Supervisable is empty");
			} else {
				endEvents.add(endEvent.get());
			}
		}
	}

	public void runAllServicesOnce() {
		flatShExecutor.runAllOnce();
	}

	public boolean isEmptyActiveServicesList() {
		return flatShExecutor.isEmpty();
	}

	@Override
	public BackgroundService createService(final String name,
										   final String spoolName,
										   final RunnableWithException task,
										   final RunnableWithException disableTask) {
		log.debug("Create service {}, spool {}", name, spoolName);
		disableTaskList.add(disableTask);
		return new FlatBackgroundService(flatShExecutor, task.toRunnable(), disableTask);
	}

	@Override
	public BackgroundService startService(final String name,
										  final String spoolName,
										  final Duration duration,
										  final RunnableWithException task,
										  final RunnableWithException disableTask) {
		log.debug("Start service {}, spool {}", name, spoolName);
		return createService(name, spoolName, task, disableTask)
				.setTimedInterval(duration).enable();
	}

	@Override
	public BackgroundService startService(final String name,
										  final String spoolName,
										  final long timedInterval,
										  final TimeUnit unit,
										  final RunnableWithException task,
										  final RunnableWithException disableTask) {
		log.debug("Start service {}, spool {}", name, spoolName);
		return createService(name, spoolName, task, disableTask)
				.setTimedInterval(timedInterval, unit).enable();
	}

	@Override
	public Spooler getSpooler() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean runOneShot(final Job job) {
		final var name = job.getJobName();
		final var spoolName = job.getJobSpoolname();

		var supervisable = new Supervisable(spoolName, "BEFORE " + name, supervisableEvents);
		manuallyRegistedSupervisables.set(supervisable);
		job.onJobStart();
		try {
			job.run();
			supervisable.end();

			supervisable = new Supervisable(spoolName, AFTER + name, supervisableEvents);
			manuallyRegistedSupervisables.set(supervisable);
			job.onJobDone();
			supervisable.end();
		} catch (final Exception e) {
			supervisable.end(e);

			supervisable = new Supervisable(spoolName, AFTER + name, supervisableEvents);
			manuallyRegistedSupervisables.set(supervisable);
			job.onJobFail(e);
			supervisable.end(e);
		}
		manuallyRegistedSupervisables.remove();
		return true;
	}

	@Override
	public boolean runOneShot(final String name,
							  final String spoolName,
							  final int priority,
							  final RunnableWithException task,
							  final Consumer<Exception> afterRunCommand) {
		var supervisable = new Supervisable(spoolName, "BEFORE " + name, supervisableEvents);
		manuallyRegistedSupervisables.set(supervisable);
		try {
			task.run();
			supervisable.end();

			supervisable = new Supervisable(spoolName, AFTER + name, supervisableEvents);
			manuallyRegistedSupervisables.set(supervisable);
			afterRunCommand.accept(null);
			supervisable.end();
		} catch (final Exception e) {
			supervisable.end(e);

			supervisable = new Supervisable(spoolName, AFTER + name, supervisableEvents);
			manuallyRegistedSupervisables.set(supervisable);
			afterRunCommand.accept(e);
			supervisable.end(e);
		}
		manuallyRegistedSupervisables.remove();
		return true;
	}

	@Override
	public void shutdown() {
		disableTaskList.forEach(d -> d.toRunnable().run());
	}

	@Override
	public void onApplicationReadyRunBackgroundServices() {
		/**
		 * Not needed, not implemented
		 */
	}

	public List<SupervisableEndEvent> getEndEventsList() {
		return endEvents;
	}

	/**
	 * Only for test purpose ! The supplied Supervisable will never be processed.
	 */
	public static void setManuallyRegistedSupervisable(final Supervisable supervisable) {
		manuallyRegistedSupervisables.set(supervisable);
	}

}
