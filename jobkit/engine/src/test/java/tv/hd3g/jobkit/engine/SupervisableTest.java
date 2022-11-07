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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.INTERNAL_STATE_CHANGE;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.SECURITY;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.TRIVIAL;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.URGENT;
import static tv.hd3g.jobkit.engine.SupervisableResultState.NOTHING_TO_DO;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_CANCELED;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_DONE;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import net.datafaker.Faker;

class SupervisableTest {
	static Faker faker = Faker.instance();

	Supervisable s;
	String spoolName;
	String jobName;
	String managerName;
	String typeName;
	String code;
	String defaultResult;
	Object[] vars;
	SupervisableMessage message;

	@Mock
	SupervisableEvents events;
	@Mock
	Exception exception;
	@Mock
	JsonNode context;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	Object businessObject;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		spoolName = faker.chuckNorris().fact();
		jobName = faker.australia().locations();
		managerName = faker.backToTheFuture().character();
		typeName = faker.aviation().airport();
		vars = new String[] { faker.australia().animals(), faker.australia().animals() };
		code = faker.ancient().primordial();
		defaultResult = faker.basketball().positions();
		message = new SupervisableMessage(code, defaultResult, vars);

		s = new Supervisable(spoolName, jobName, events).setContext(typeName, NullNode.getInstance());
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(events, exception, context, objectMapper, businessObject);
	}

	@Test
	void testGetSupervisable_fail() {
		assertThrows(IllegalThreadStateException.class, Supervisable::getSupervisable);
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testGetSupervisable_ok() throws InterruptedException {
		final var jk = new JobKitEngine(Executors.newScheduledThreadPool(1),
				new ExecutionEvent() {},
				new BackgroundServiceEvent() {},
				new SupervisableManager(managerName));

		final var arSupervisable = new AtomicReference<Supervisable>();
		final var arException = new AtomicReference<Exception>();

		final var latch = new CountDownLatch(1);

		jk.runOneShot(jobName, spoolName, 0, () -> {
			final var su = Supervisable.getSupervisable();
			su.resultDone();
			arSupervisable.set(su);
			latch.countDown();
		}, e -> {
			arException.set(e);
			latch.countDown();
		});

		latch.await(1, TimeUnit.SECONDS);
		assertNotEquals(s, arSupervisable.get());
		assertNull(arException.get());
		s = arSupervisable.get();
		assertNotNull(s);
		final var eEvent = s.getEndEvent(Optional.empty(), managerName).get();
		assertNotNull(eEvent);
		assertEquals(jobName, eEvent.jobName());
		assertEquals(spoolName, eEvent.spoolName());
		assertEquals(WORKS_DONE, eEvent.result().state());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testGetSupervisable_ok_empty() throws InterruptedException {
		final var jk = new JobKitEngine(Executors.newScheduledThreadPool(1),
				new ExecutionEvent() {},
				new BackgroundServiceEvent() {},
				new SupervisableManager(managerName));

		final var arSupervisable = new AtomicReference<Supervisable>();
		final var arException = new AtomicReference<Exception>();

		final var latch = new CountDownLatch(1);

		jk.runOneShot(jobName, spoolName, 0, () -> {
			arSupervisable.set(Supervisable.getSupervisable());
			latch.countDown();
		}, e -> {
			arException.set(e);
			latch.countDown();
		});

		latch.await(1, TimeUnit.SECONDS);
		assertNotEquals(s, arSupervisable.get());
		assertNull(arException.get());
		s = arSupervisable.get();
		assertNotNull(s);
		assertTrue(s.getEndEvent(Optional.empty(), managerName).isEmpty());
	}

	static void checkCaller(final StackTraceElement stepCaller) {
		assertNotNull(stepCaller);
		final var thisCaller = Supervisable.createCaller(2);
		assertEquals(thisCaller.getClassLoaderName(), stepCaller.getClassLoaderName());
		assertEquals(thisCaller.getClassName(), stepCaller.getClassName());
		assertEquals(thisCaller.getFileName(), stepCaller.getFileName());
		assertEquals(thisCaller.getMethodName(), stepCaller.getMethodName());
		assertEquals(thisCaller.getModuleName(), stepCaller.getModuleName());
		assertEquals(thisCaller.getModuleVersion(), stepCaller.getModuleVersion());
	}

	SupervisableEndEvent getEndEvent() {
		final var event = s.getEndEvent(Optional.empty(), managerName).get();
		assertEquals(managerName, event.managerName());
		assertNotNull(event.creationDate());
		return event;
	}

	SupervisableStep getLastStep() {
		final var steps = getEndEvent().steps();
		assertEquals(1, steps.size());
		return steps.get(0);
	}

	void assertNoSteps() {
		assertTrue(getEndEvent().steps().isEmpty());
	}

	@Test
	void testStart() {
		s.start();
		assertTrue(s.toString().contains("PROCESS"));
		assertNoSteps();
		final var event = getEndEvent();
		assertNotNull(event.startDate());
		assertNull(event.endDate());
		assertNull(event.error());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testEnd() {
		s.end();
		assertTrue(s.toString().contains("DONE"));
		verify(events, times(1)).onEnd(s, Optional.empty());
		assertNoSteps();
		final var event = getEndEvent();
		assertNull(event.startDate());
		assertNotNull(event.endDate());
		assertNull(event.error());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testEndException() {
		s.end(exception);

		assertTrue(s.toString().contains("ERROR"));
		verify(events, times(1)).onEnd(s, Optional.ofNullable(exception));
		assertNoSteps();
		final var event = getEndEvent();
		assertNull(event.startDate());
		assertNotNull(event.endDate());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testEndEventException() {
		final var event = s.getEndEvent(Optional.ofNullable(exception), managerName).get();
		assertEquals(managerName, event.managerName());
		assertEquals(exception, event.error());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testResultError() {
		s.resultError(exception);
		s.end();

		assertTrue(s.toString().contains("ERROR"));
		verify(events, times(1)).onEnd(s, Optional.ofNullable(exception));
		assertNoSteps();
		final var event = getEndEvent();
		assertNull(event.startDate());
		assertNotNull(event.endDate());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testOnMessage() {
		s.onMessage(code, defaultResult, vars);

		final var step = getLastStep();
		assertNotNull(step.stepDate());
		assertEquals(message, step.message());
		checkCaller(step.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testSetContextStringJsonNode() {
		s.setContext(typeName, context);
		final var endEvent = getEndEvent();
		assertEquals(context, endEvent.context());
		assertEquals(typeName, endEvent.typeName());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testSetContextStringObject() {
		when(events.extractContext(businessObject)).thenReturn(context);

		s.setContext(typeName, businessObject);
		final var endEvent = getEndEvent();
		assertEquals(context, endEvent.context());
		assertEquals(typeName, endEvent.typeName());
		verify(events, times(1)).extractContext(businessObject);
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testSetContextVarargs() {
		when(events.extractContext(any(LinkedHashMap.class))).thenReturn(context);

		final var k0 = faker.numerify("K_###");
		final var k1 = faker.numerify("K_###");
		final var k2 = faker.numerify("K_###");
		final var k3 = faker.numerify("K_###");
		final var v0 = faker.numerify("V_###");
		final var v1 = faker.numerify("V_###");
		final var v2 = faker.numerify("V_###");
		final var v3 = faker.numerify("V_###");

		s.setContext(typeName, k0, v0, k1, v1, k2, v2, k3, v3);
		final var endEvent = getEndEvent();
		assertEquals(context, endEvent.context());
		assertEquals(typeName, endEvent.typeName());
		verify(events, times(1)).extractContext(any(LinkedHashMap.class));
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testSetContextVarargs_object() {
		when(events.extractContext(any(LinkedHashMap.class))).thenReturn(context);

		final var k0 = new Object();
		final var v0 = faker.numerify("V_###");

		s.setContext(typeName, k0, v0);
		final var endEvent = getEndEvent();
		assertEquals(context, endEvent.context());
		assertEquals(typeName, endEvent.typeName());
		verify(events, times(1)).extractContext(any(LinkedHashMap.class));
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testSetContextVarargs_invalid() {
		final var k0 = faker.numerify("K_###");

		assertThrows(IllegalArgumentException.class, () -> s.setContext(typeName, k0, k0, k0));
		final var endEvent = getEndEvent();
		assertEquals(NullNode.getInstance(), endEvent.context());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testSetContextVarargs_empty() {
		assertThrows(IllegalArgumentException.class, () -> s.setContext(typeName));
		final var endEvent = getEndEvent();
		assertEquals(NullNode.getInstance(), endEvent.context());
		assertEquals(Set.of(TRIVIAL), getEndEvent().marks());
	}

	@Test
	void testResultDoneString() {
		s.resultDone(code, defaultResult, vars);
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertEquals(message, r.message());
		assertEquals(WORKS_DONE, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testResultDone() {
		s.resultDone();
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertNull(r.message());
		assertEquals(WORKS_DONE, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testResultCanceledString() {
		s.resultCanceled(code, defaultResult, vars);
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertEquals(message, r.message());
		assertEquals(WORKS_CANCELED, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testResultCanceled() {
		s.resultCanceled();
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertNull(r.message());
		assertEquals(WORKS_CANCELED, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testResultNothingToDoString() {
		s.resultNothingToDo(code, defaultResult, vars);
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertEquals(message, r.message());
		assertEquals(NOTHING_TO_DO, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testResultNothingToDo() {
		s.resultNothingToDo();
		final var r = getEndEvent().result();
		assertNotNull(r.date());
		assertNull(r.message());
		assertEquals(NOTHING_TO_DO, r.state());
		checkCaller(r.caller());
		assertEquals(Set.of(), getEndEvent().marks());
	}

	@Test
	void testToString() {
		final var result = s.toString();
		assertTrue(result.contains("NEW"));
		assertTrue(result.contains(jobName));
		assertTrue(result.contains(spoolName));
	}

	@Test
	void testMarkAsUrgent() {
		s.markAsUrgent();
		assertEquals(Set.of(URGENT), getEndEvent().marks());
	}

	@Test
	void testMarkAsInternalStateChange() {
		s.markAsInternalStateChange();
		assertEquals(Set.of(INTERNAL_STATE_CHANGE), getEndEvent().marks());
	}

	@Test
	void testMarkAsSecurity() {
		s.markAsSecurity();
		assertEquals(Set.of(SECURITY), getEndEvent().marks());
	}

}
