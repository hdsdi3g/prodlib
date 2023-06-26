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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.engine;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogSpoolReport;

@Slf4j
public class SupervisableManager implements SupervisableEvents, SupervisableEventRegister {

	private static final String JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE = "jobkit.watchdogspool.warning.message";
	private static final String JOBKIT_WATCHDOGSPOOL_RELEASE_MESSAGE = "jobkit.watchdogspool.warning.releasemessage";

	private final String name;
	private final ObjectMapper objectMapper;
	private final AtomicBoolean shutdown;
	private final Set<SupervisableOnEndEventConsumer> onEndEventConsumers;
	private final ArrayDeque<SupervisableEndEvent> lastEndEvents;
	private final int maxEndEventsRetention;

	public SupervisableManager(final String name, final ObjectMapper objectMapper, final int maxEndEventsRetention) {
		this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "\"objectMapper\" can't to be null");
		shutdown = new AtomicBoolean(false);
		onEndEventConsumers = Collections.synchronizedSet(new HashSet<>());
		this.maxEndEventsRetention = maxEndEventsRetention;
		lastEndEvents = new ArrayDeque<>();
	}

	public SupervisableManager(final String name) {
		this(name, new ObjectMapper(), 10);
	}

	@Override
	public SupervisableContextExtractor createContextExtractor(final SupervisableEndEvent event) {
		return new SupervisableContextExtractor(this, event);
	}

	static SupervisableEvents voidSupervisableEvents() {
		/**
		 * Do nothing
		 */
		return new SupervisableEvents() {};
	}

	void close() {
		if (shutdown.get() == false) {
			log.info("Close SupervisableManager {}", name);
			shutdown.set(true);
		}
	}

	@Override
	public void registerOnEndEventConsumer(final SupervisableOnEndEventConsumer onEndEventConsumer) {
		Objects.requireNonNull(onEndEventConsumer, "\"onEndEventConsumer\" can't to be null");
		onEndEventConsumers.add(onEndEventConsumer);

		synchronized (lastEndEvents) {
			lastEndEvents.forEach(onEndEventConsumer::afterProcess);
		}
	}

	@Override
	public void onEnd(final Supervisable supervisable, final Optional<Exception> oError) {
		final var oEndEvent = supervisable.getEndEvent(oError, name);
		if (oEndEvent.isEmpty()) {
			log.trace("Supervisable is empty");
			return;
		}
		final var endEvent = oEndEvent.get();

		if (shutdown.get()) {
			log.error("Can't manage event [onEnd/{}] on a closed SupervisableManager", supervisable);
			return;
		}
		log.trace("Queue end event for {}", supervisable);

		synchronized (lastEndEvents) {
			while (lastEndEvents.size() >= maxEndEventsRetention) {
				lastEndEvents.pollLast();
			}
			lastEndEvents.push(endEvent);
		}

		try {
			onEndEventConsumers.forEach(event -> event.afterProcess(endEvent));
		} catch (final Exception e) {
			log.error("Can't queue end event", e);
		}
	}

	@Override
	public void onJobWatchdogSpoolReport(final JobWatchdogSpoolReport report) {
		final var s = new Supervisable(report.spoolName(), report.activeJob().commandName(), this);

		s.start();
		s.markAsUrgent();
		s.markAsInternalStateChange();

		var i = 0;
		s.onMessage(
				JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + i++,
				"For information, a current job execution spooler (\"{0}\") seams to be have some troubles, maybe the current running task is blocked, with {1} waiting task(s). The warning report say: \"{2}\"",
				report.spoolName(),
				report.queuedJobs().size(),
				report.warning().getMessage());

		s.onMessage(
				JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + i++,
				"The current running job \"{0}\" was created the {1}, started the {2}, by \"{3}\".",
				report.activeJob().commandName(),
				report.activeJob().createdDate(),
				report.activeJob().startedDate().map(Date::new).orElse(null),
				report.activeJob().creator().map(StackTraceElement::toString).orElse("(source unknown)"));

		final var queuedJobs = report.queuedJobs().stream()
				.sorted((l, r) -> Long.compare(l.createdIndex(), r.createdIndex())).toList();
		if (queuedJobs.isEmpty() == false) {
			s.onMessage(
					JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + i++,
					"The older queued (waiting) job in this spooler was created the {0}.",
					queuedJobs.get(0).createdDate());
		}
		if (queuedJobs.size() > 1) {
			s.onMessage(
					JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + i++, // NOSONAR S1854
					"The most recent queued (waiting) job in this spooler was created the {0}.",
					queuedJobs.get(queuedJobs.size() - 1).createdDate());
		}

		if (report.relativeBackgroundServices().isEmpty() == false) {
			s.onMessage(
					JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + i++, // NOSONAR S1854
					"Some jobs (or the totality) was created by one of those application internal service:");

			report.relativeBackgroundServices()
					.forEach(b -> s.onMessage(
							JOBKIT_WATCHDOGSPOOL_WARNING_MESSAGE + ".bckservice", // NOSONAR S1854
							"{0}, runned every {1} after the last runned job",
							b.serviceName(), Duration.ofMillis(b.timedInterval()))

					);
		}

		s.resultDone(
				"jobkit.watchdogspool.warning.policy." + report.policy().getClass().getSimpleName().toLowerCase(),
				"[Execution spool warning] {0} on \"{1}\"",
				report.policy().getDescription(),
				report.spoolName());
		s.setContext(JobWatchdogSpoolReport.class.getName(), report);
		s.end();
	}

	@Override
	public void onJobWatchdogSpoolReleaseReport(final JobWatchdogSpoolReport report) {
		final var s = new Supervisable(report.spoolName(), report.activeJob().commandName(), this);

		s.start();
		s.markAsUrgent();
		s.markAsInternalStateChange();

		s.onMessage(
				JOBKIT_WATCHDOGSPOOL_RELEASE_MESSAGE,
				"For information, a job execution spooler (\"{0}\") was triggered an alert. This alert is now closed, the queue resumed a normal activity. The warning report was said: \"{1}\"",
				report.spoolName(),
				report.warning().getMessage());

		s.resultDone(
				"jobkit.watchdogspool.warning.policy." + report.policy().getClass().getSimpleName().toLowerCase(),
				"[Problem closed] {0} on \"{1}\"",
				report.policy().getDescription(),
				report.spoolName());
		s.setContext(JobWatchdogSpoolReport.class.getName(), report);
		s.end();
	}

	@Override
	public JsonNode extractContext(final Object businessObject) {
		log.trace("Extract {} / {}...", businessObject, businessObject.getClass().getName());
		final var result = objectMapper.valueToTree(businessObject);
		log.trace("...extract result {} / {}: {}", businessObject, businessObject.getClass().getName(), result);
		return result;
	}

	@Override
	public <T> T getBusinessObject(final JsonNode context, final Class<T> type) throws JsonProcessingException {
		log.trace("Read {} / {}...", context, type.getName());
		final var result = objectMapper.treeToValue(context, type);
		log.trace("...to {}", result);
		return result;
	}

	@Override
	public String toString() {
		return name;
	}

}
