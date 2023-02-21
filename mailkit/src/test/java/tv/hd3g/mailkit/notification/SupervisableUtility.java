/*
 * This file is part of mailkit.
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
package tv.hd3g.mailkit.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.internal.util.MockUtil.isMock;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.jobkit.engine.SupervisableEventMark;
import tv.hd3g.jobkit.engine.SupervisableMessage;
import tv.hd3g.jobkit.engine.SupervisableResult;
import tv.hd3g.jobkit.engine.SupervisableResultState;
import tv.hd3g.jobkit.engine.SupervisableStep;

public class SupervisableUtility {
	static Faker faker = Faker.instance();

	public final SupervisableResultState rState;
	public final Locale lang;
	public final SupervisableStep step;
	public final SupervisableMessage message;
	public final SupervisableResult sResult;
	public final SupervisableEndEvent event;
	public final String spoolName;
	public final String jobName;
	public final String typeName;
	public final String managerName;
	public final Date date;
	public final Set<SupervisableEventMark> marks;

	private SupervisableUtility(final Locale lang,
								final String spoolName,
								final String jobName,
								final String typeName,
								final String managerName,
								final Date date,
								final Set<SupervisableEventMark> marks,
								final StackTraceElement caller,
								final Exception error,
								final JsonNode context,
								final List<SupervisableStep> steps,
								final SupervisableResult sResult) {
		this.lang = lang;
		this.spoolName = spoolName;
		this.jobName = jobName;
		this.typeName = typeName;
		this.managerName = managerName;
		this.date = date;
		this.sResult = sResult;
		this.marks = marks;
		event = new SupervisableEndEvent(spoolName, jobName, typeName, managerName, context, date, date, date,
				steps, sResult, error, marks);
		rState = Optional.ofNullable(sResult).map(SupervisableResult::state).orElse(null);
		step = Optional.ofNullable(steps).stream().flatMap(List::stream).findFirst().orElse(null);
		message = Optional.ofNullable(sResult).map(SupervisableResult::message).orElse(null);
	}

	private SupervisableUtility(final SupervisableResultState rState,
								final Locale lang,
								final SupervisableMessage message,
								final String spoolName,
								final String jobName,
								final String typeName,
								final String managerName,
								final Date date,
								final Set<SupervisableEventMark> marks,
								final StackTraceElement caller,
								final Exception error,
								final JsonNode context) {
		this(lang, spoolName, jobName, typeName, managerName, date, marks, caller, error, context,
				List.of(new SupervisableStep(date, message, caller)),
				new SupervisableResult(date, rState, message, caller));
	}

	public static SupervisableUtility aLaCarte(final boolean containError,
											   final boolean containResult,
											   final boolean containSteps) {
		final var message = new SupervisableMessage(
				faker.numerify("code###"),
				faker.numerify("default"),
				List.of());
		final var caller = mock(StackTraceElement.class);
		final var rState = faker.options().option(SupervisableResultState.values());

		return new SupervisableUtility(
				getLang(), faker.numerify("spoolName###"),
				faker.numerify("jobName###"),
				faker.numerify("typeName###"),
				faker.numerify("managerName###"),
				new Date(),
				Set.of(),
				caller,
				containError ? mock(Exception.class) : null,
				mock(JsonNode.class),
				containSteps ? List.of(new SupervisableStep(new Date(), message, caller)) : null,
				containResult ? new SupervisableResult(new Date(), rState, message, caller) : null);
	}

	public SupervisableUtility(final StackTraceElement caller,
							   final Exception error,
							   final JsonNode context) {
		this(
				faker.options().option(SupervisableResultState.values()),
				getLang(),
				new SupervisableMessage(
						faker.numerify("code###"),
						faker.numerify("default"),
						List.of()),
				faker.numerify("spoolName###"),
				faker.numerify("jobName###"),
				faker.numerify("typeName###"),
				faker.numerify("managerName###"),
				new Date(),
				Set.of(),
				caller,
				error,
				context);
	}

	public SupervisableUtility() {
		this(mock(StackTraceElement.class), mock(Exception.class), mock(JsonNode.class));
	}

	public static SupervisableUtility withMarks(final Set<SupervisableEventMark> marks, final boolean containError) {
		return new SupervisableUtility(
				faker.options().option(SupervisableResultState.values()),
				getLang(),
				new SupervisableMessage(
						faker.numerify("code###"),
						faker.numerify("default"),
						List.of()),
				faker.numerify("spoolName###"),
				faker.numerify("jobName###"),
				faker.numerify("typeName###"),
				faker.numerify("managerName###"),
				new Date(),
				marks,
				mock(StackTraceElement.class),
				containError ? mock(Exception.class) : null,
				mock(JsonNode.class));
	}

	public void verifyNoMoreInteractions() {
		if (step != null && step.caller() != null && isMock(step.caller())) {
			Mockito.verifyNoMoreInteractions(step.caller());
		}
		if (event.error() != null && isMock(event.error())) {
			Mockito.verifyNoMoreInteractions(event.error());
		}
		if (event.context() != null && isMock(event.context())) {
			Mockito.verifyNoMoreInteractions(event.context());
		}
	}

	public static Locale getLang() {
		return faker.options().option(Locale.getAvailableLocales());
	}

}
