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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.jobkit.engine.FlatJobKitEngine.manuallyRegistedSupervisables;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.INTERNAL_STATE_CHANGE;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.SECURITY;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.TRIVIAL;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.URGENT;
import static tv.hd3g.jobkit.engine.SupervisableResultState.NOTHING_TO_DO;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_CANCELED;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_DONE;
import static tv.hd3g.jobkit.engine.SupervisableState.DONE;
import static tv.hd3g.jobkit.engine.SupervisableState.ERROR;
import static tv.hd3g.jobkit.engine.SupervisableState.NEW;
import static tv.hd3g.jobkit.engine.SupervisableState.PROCESS;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

public class Supervisable {
	private static Logger log = LogManager.getLogger();

	public static final Supervisable getSupervisable() {
		final var t = Thread.currentThread();
		if (t instanceof final SpoolJobStatus status) {
			log.trace("Get for {}", status);
			return status.getSupervisable();
		} else {
			final var manually = manuallyRegistedSupervisables.get();
			if (manually == null) {
				throw new IllegalThreadStateException("Can't extract Supervisable, current Thread " + t.getName()
													  + " is not a SpoolJobStatus: " + t.getClass().getName());
			} else {
				/**
				 * Should used only in FlatJobKitEngine
				 */
				return manually;
			}
		}
	}

	private final String spoolName;
	private final String jobName;
	private final SupervisableEvents events;
	private final BlockingQueue<SupervisableStep> steps;
	private final Date creationDate;
	private final Set<SupervisableEventMark> marks;

	private Date startDate;
	private SupervisableState supervisableState;
	private Date endDate;
	private JsonNode context;
	private String typeName;
	private SupervisableResult sResult;
	private Exception error;

	Supervisable(final String spoolName, final String jobName, final SupervisableEvents events) {
		this.spoolName = spoolName;
		this.jobName = jobName;
		this.events = events;

		steps = new LinkedBlockingQueue<>();
		creationDate = new Date();
		supervisableState = NEW;
		marks = new HashSet<>();
		marks.add(TRIVIAL);
		log.trace("Create Supervisable [{}/{}] {}", spoolName, jobName, supervisableState);
	}

	synchronized void start() {
		startDate = new Date();
		supervisableState = PROCESS;
		log.trace("Start Supervisable [{}/{}] {}", spoolName, jobName, supervisableState);
	}

	synchronized void end() {
		if (error != null) {
			end(error);
		} else {
			supervisableState = DONE;
			endDate = new Date();
			log.trace("Ends Supervisable [{}/{}] {}", spoolName, jobName, supervisableState);
			events.onEnd(this, Optional.empty());
		}
	}

	synchronized void end(final Exception e) {
		supervisableState = ERROR;
		marks.remove(TRIVIAL);
		endDate = new Date();
		log.trace("Ends Supervisable [{}/{}] with exception {}", spoolName, jobName, supervisableState, e);
		events.onEnd(this, Optional.ofNullable(e));
	}

	/**
	 * @param defaultResult beware of use "{0}, {1}" instead of just "{}".
	 */
	public synchronized Supervisable onMessage(final String code,
											   final String defaultResult,
											   final Object... vars) {
		log.trace("{} \"{}\": {}", supervisableState, code, vars);

		steps.add(new SupervisableStep(new Date(),
				new SupervisableMessage(code, defaultResult, vars),
				createCaller(2)));
		marks.remove(TRIVIAL);
		return this;
	}

	public synchronized Supervisable setContext(final String typeName, final JsonNode context) {
		this.typeName = Objects.requireNonNull(typeName, "\"typeName\" can't to be null");
		this.context = Objects.requireNonNull(context, "\"context\" can't to be null");
		log.trace("{} typeName: \"{}\" context: \"{}\"", supervisableState, typeName, context);
		return this;
	}

	public synchronized Supervisable setContext(final String typeName, final Object businessObject) {
		setContext(typeName, events.extractContext(businessObject));
		return this;
	}

	/**
	 * @param keysValues must be pair [k0, v0, k1, v1, k2, v2...] will be putted on the root of generated Json
	 */
	public synchronized Supervisable setContext(final String typeName,
												final Object... keysValues) {
		if (keysValues == null
			|| keysValues.length == 0
			|| keysValues.length % 2 != 0) {
			throw new IllegalArgumentException("keysValues must by pair: k0, v0, k1, v1, k2, v2...");
		}

		final var map = new LinkedHashMap<>(keysValues.length / 2);
		for (var pos = 0; pos < keysValues.length; pos += 2) {
			final var key = Objects.requireNonNull(keysValues[pos], "keysValues[" + pos + "] is null !");
			if (key instanceof String) {
				map.put(key, keysValues[pos + 1]);
			} else {
				map.put(String.valueOf(key), keysValues[pos + 1]);
			}
		}
		return setContext(typeName, events.extractContext(map));
	}

	/**
	 * Works is done
	 */
	public synchronized void resultDone(final String code,
										final String defaultResult,
										final Object... vars) {
		sResult = new SupervisableResult(new Date(), WORKS_DONE,
				new SupervisableMessage(code, defaultResult, vars), createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * Works is done
	 */
	public synchronized void resultDone() {
		sResult = new SupervisableResult(new Date(), WORKS_DONE, null, createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * Works is not done, but this is not an app problem
	 */
	public synchronized void resultCanceled(final String code,
											final String defaultResult,
											final Object... vars) {
		sResult = new SupervisableResult(new Date(), WORKS_CANCELED,
				new SupervisableMessage(code, defaultResult, vars), createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * Works is not done, but this is not an app problem
	 */
	public synchronized void resultCanceled() {
		sResult = new SupervisableResult(new Date(), WORKS_CANCELED, null, createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * No works to not done, this is not a problem
	 */
	public synchronized void resultNothingToDo(final String code,
											   final String defaultResult,
											   final Object... vars) {
		sResult = new SupervisableResult(new Date(), NOTHING_TO_DO,
				new SupervisableMessage(code, defaultResult, vars), createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * No works to not done, this is not a problem
	 */
	public synchronized void resultNothingToDo() {
		sResult = new SupervisableResult(new Date(), NOTHING_TO_DO, null, createCaller(2));
		marks.remove(TRIVIAL);
	}

	/**
	 * This is an app problem
	 */
	public synchronized void resultError(final Exception error) {
		this.error = error;
		marks.remove(TRIVIAL);
	}

	public synchronized Supervisable markAsUrgent() {
		marks.remove(TRIVIAL);
		marks.add(URGENT);
		return this;
	}

	public synchronized Supervisable markAsInternalStateChange() {
		marks.remove(TRIVIAL);
		marks.add(INTERNAL_STATE_CHANGE);
		return this;
	}

	public synchronized Supervisable markAsSecurity() {
		marks.remove(TRIVIAL);
		marks.add(SECURITY);
		return this;
	}

	@Override
	public synchronized String toString() {
		final var builder = new StringBuilder();
		builder.append("Supervisable [spoolName=");
		builder.append(spoolName);
		builder.append(", jobName=");
		builder.append(jobName);
		builder.append(", typeName=");
		builder.append(typeName);
		builder.append(", supervisableState=");
		builder.append(supervisableState);
		builder.append("]");
		return builder.toString();
	}

	static StackTraceElement createCaller(final int relative) {
		return Stream.of(new Throwable().getStackTrace())
				.skip(relative)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Can't found caller"));
	}

	synchronized SupervisableEndEvent getEndEvent(final Optional<Exception> oError, final String managerName) {
		return new SupervisableEndEvent(
				spoolName,
				jobName,
				typeName,
				managerName,
				context,
				creationDate,
				startDate,
				endDate,
				steps.stream().toList(),
				sResult,
				oError.orElse(null),
				marks.stream().collect(toUnmodifiableSet()));
	}

}
