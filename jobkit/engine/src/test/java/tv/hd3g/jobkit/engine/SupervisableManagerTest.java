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

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_DONE;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicy;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicyWarning;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogSpoolReport;
import tv.hd3g.jobkit.engine.watchdog.WatchableBackgroundService;
import tv.hd3g.jobkit.engine.watchdog.WatchableSpoolJobState;

class SupervisableManagerTest {
	static Faker faker = Faker.instance();

	SupervisableManager s;
	String name;

	@Mock
	ObjectMapper objectMapper;
	@Mock
	Object businessObject;
	@Mock
	JsonNode context;
	@Mock
	Supervisable supervisable;
	@Mock
	Optional<Exception> oError;
	@Mock
	SupervisableOnEndEventConsumer eventConsumer;
	@Mock
	SupervisableEndEvent supervisableEndEvent;
	@Mock
	SupervisableOnEndEventConsumer eventConsumer2;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = faker.company().name();
		s = new SupervisableManager(name, objectMapper, 1);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(
				objectMapper,
				businessObject,
				context,
				supervisable,
				oError,
				eventConsumer,
				supervisableEndEvent,
				eventConsumer2);
	}

	@Test
	void testGetLifeCycle_extractContext() {
		when(objectMapper.valueToTree(businessObject)).thenReturn(context);
		final var result = s.extractContext(businessObject);
		verify(objectMapper, times(1)).valueToTree(businessObject);
		assertEquals(context, result);
	}

	@Test
	void testGetLifeCycle_extractContext_defaultObjectMapper() {
		s = new SupervisableManager(name);

		final var text = faker.artist().name();
		final var result = s.extractContext(List.of(text));
		assertEquals("[\"" + text + "\"]", result.toString());
	}

	@Test
	void testGetBusinessObject() throws JsonProcessingException, IllegalArgumentException {
		when(objectMapper.treeToValue(context, Object.class)).thenReturn(businessObject);
		final var result = s.getBusinessObject(context, Object.class);
		verify(objectMapper, times(1)).treeToValue(context, Object.class);
		assertEquals(businessObject, result);
	}

	@Test
	void testGetLifeCycle_onEnd() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.registerOnEndEventConsumer(eventConsumer);

		s.onEnd(supervisable, oError);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_Exception() {

		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.registerOnEndEventConsumer(eventConsumer);

		Mockito.doThrow(new IllegalStateException()).when(eventConsumer).afterProcess(supervisableEndEvent);
		s.onEnd(supervisable, oError);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_close() {
		s.close();
		s.onEnd(supervisable, oError);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testVoidSupervisableEvents() {
		assertNotNull(SupervisableManager.voidSupervisableEvents());
	}

	@Test
	void testToString() {
		assertNotNull(s.toString());
	}

	@Test
	void testCreateContextExtractor() {
		assertNotNull(s.createContextExtractor(supervisableEndEvent));
	}

	@Test
	void testGetLifeCycle_onEnd_retention() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.onEnd(supervisable, oError);
		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_limited_retention() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.onEnd(supervisable, oError);
		s.onEnd(supervisable, oError);
		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(2)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_non_limited_retention() {
		final var iterations = faker.random().nextInt(5, 100);
		s = new SupervisableManager(name, objectMapper, iterations);

		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		for (var pos = 0; pos < iterations; pos++) {
			s.onEnd(supervisable, oError);
		}
		s.onEnd(supervisable, oError);

		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(iterations)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(iterations + 1)).getEndEvent(oError, name);

		s.registerOnEndEventConsumer(eventConsumer2);
		verify(eventConsumer2, times(iterations)).afterProcess(supervisableEndEvent);
	}

	private static class Policy implements JobWatchdogPolicy {
		@Override
		public void isStatusOk(final String spoolName,
							   final WatchableSpoolJobState activeJob,
							   final Set<WatchableSpoolJobState> queuedJobs,
							   final Set<WatchableBackgroundService> relativeBackgroundServices) throws JobWatchdogPolicyWarning {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Duration> isStatusOk(final String spoolName,
											 final WatchableSpoolJobState activeJob,
											 final Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getDescription() {
			return faker.numerify("policyDescription###");
		}
	}

	@Nested
	class Report {

		JobWatchdogSpoolReport report;

		String spoolName;
		WatchableSpoolJobState activeJob;
		Date createdDate;
		Set<WatchableSpoolJobState> queuedJobs;
		JobWatchdogPolicyWarning warning;
		Set<WatchableBackgroundService> relativeBackgroundServices;
		JobWatchdogPolicy policy;
		SupervisableEndEvent endEvent;

		WatchableSpoolJobState queuedJob0;
		WatchableSpoolJobState queuedJob1;
		WatchableBackgroundService service;

		@Captor
		ArgumentCaptor<SupervisableEndEvent> supervisableEndEventCaptor;

		@BeforeEach
		void init() throws Exception {
			openMocks(this).close();
			s.registerOnEndEventConsumer(eventConsumer);

			spoolName = faker.numerify("spoolName###");
			createdDate = new Date();
			activeJob = new WatchableSpoolJobState(
					createdDate, faker.numerify("commandName###"), -1, Optional.empty(), Optional.empty());
			warning = new JobWatchdogPolicyWarning(faker.numerify("warning###"));
			queuedJobs = Set.of();
			relativeBackgroundServices = Set.of();
			policy = new Policy();

			queuedJob0 = new WatchableSpoolJobState(
					createdDate, faker.numerify("commandName###"), -1, Optional.empty(), Optional.empty());
			queuedJob1 = new WatchableSpoolJobState(
					createdDate, faker.numerify("commandName###"), -1, Optional.empty(), Optional.empty());
			service = new WatchableBackgroundService(faker.numerify("serviceName###"),
					spoolName, faker.random().nextLong(10, 10000));
		}

		@AfterEach
		void end() {
			assertTrue(endEvent.creationDate().getTime() > 0);
			assertTrue(new Date().compareTo(endEvent.creationDate()) >= 0);
			assertTrue(endEvent.startDate().compareTo(endEvent.creationDate()) >= 0);
			assertTrue(endEvent.endDate().compareTo(endEvent.startDate()) >= 0);

			assertEquals(context, endEvent.context());
			assertTrue(endEvent.isInternalStateChangeMarked());
			assertTrue(endEvent.isNotTrivialMarked());
			assertFalse(endEvent.isSecurityMarked());
			assertTrue(endEvent.isTypeName(JobWatchdogSpoolReport.class.getName()));
			assertTrue(endEvent.isUrgentMarked());
			assertEquals(WORKS_DONE, endEvent.result().state());
			assertFalse(endEvent.result().message().code().isEmpty());
			assertFalse(endEvent.result().message().defaultResult().isEmpty());
			assertFalse(Arrays.asList(endEvent.result().message().getVarsArray()).isEmpty());
			assertEquals(spoolName, endEvent.spoolName());
			assertEquals(activeJob.commandName(), endEvent.jobName());
		}

		@Test
		void testWarnReport() {
			report = new JobWatchdogSpoolReport(
					createdDate, spoolName, activeJob, queuedJobs, policy, warning, relativeBackgroundServices);
			when(objectMapper.valueToTree(report)).thenReturn(context);

			s.onJobWatchdogSpoolReport(report);

			verify(objectMapper, times(1)).valueToTree(report);
			verify(eventConsumer, times(1)).afterProcess(supervisableEndEventCaptor.capture());
			endEvent = supervisableEndEventCaptor.getValue();

			assertEquals(2, endEvent.steps().size());
		}

		@Test
		void testWarnReportQueuedJob() {
			queuedJobs = Set.of(queuedJob0);
			report = new JobWatchdogSpoolReport(
					createdDate, spoolName, activeJob, queuedJobs, policy, warning, relativeBackgroundServices);
			when(objectMapper.valueToTree(report)).thenReturn(context);

			s.onJobWatchdogSpoolReport(report);

			verify(objectMapper, times(1)).valueToTree(report);
			verify(eventConsumer, times(1)).afterProcess(supervisableEndEventCaptor.capture());
			endEvent = supervisableEndEventCaptor.getValue();

			assertEquals(3, endEvent.steps().size());
		}

		@Test
		void testWarnReportQueuedJobs() {
			queuedJobs = Set.of(queuedJob0, queuedJob1);
			report = new JobWatchdogSpoolReport(
					createdDate, spoolName, activeJob, queuedJobs, policy, warning, relativeBackgroundServices);
			when(objectMapper.valueToTree(report)).thenReturn(context);

			s.onJobWatchdogSpoolReport(report);

			verify(objectMapper, times(1)).valueToTree(report);
			verify(eventConsumer, times(1)).afterProcess(supervisableEndEventCaptor.capture());
			endEvent = supervisableEndEventCaptor.getValue();

			assertEquals(4, endEvent.steps().size());
		}

		@Test
		void testWarnReportServices() {
			relativeBackgroundServices = Set.of(service);
			report = new JobWatchdogSpoolReport(
					createdDate, spoolName, activeJob, queuedJobs, policy, warning, relativeBackgroundServices);
			when(objectMapper.valueToTree(report)).thenReturn(context);

			s.onJobWatchdogSpoolReport(report);

			verify(objectMapper, times(1)).valueToTree(report);
			verify(eventConsumer, times(1)).afterProcess(supervisableEndEventCaptor.capture());
			endEvent = supervisableEndEventCaptor.getValue();

			assertEquals(4, endEvent.steps().size());
		}

		@Test
		void testReleaseReport() {
			report = new JobWatchdogSpoolReport(
					createdDate, spoolName, activeJob, queuedJobs, policy, warning, relativeBackgroundServices);
			when(objectMapper.valueToTree(report)).thenReturn(context);

			s.onJobWatchdogSpoolReleaseReport(report);

			verify(objectMapper, times(1)).valueToTree(report);
			verify(eventConsumer, times(1)).afterProcess(supervisableEndEventCaptor.capture());
			endEvent = supervisableEndEventCaptor.getValue();

			assertEquals(1, endEvent.steps().size());
		}

	}
}
