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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class FlatJobKitEngineTest {

	FlatJobKitEngine jobKitEngine;
	RunnableWithException task;
	RunnableWithException disableTask;

	@BeforeEach
	void init() {
		jobKitEngine = new FlatJobKitEngine();
		task = () -> {
		};
		disableTask = () -> {
		};

		/**
		 * Do nothing
		 */
		jobKitEngine.shutdown();
	}

	@Test
	void testCreateService() {
		assertNotNull(jobKitEngine.createService(null, null, task, disableTask));
	}

	@Test
	void testStartServiceStringStringLongTimeUnitRunnable() {
		assertNotNull(jobKitEngine.startService(null, null, 0, TimeUnit.DAYS, task, disableTask));
	}

	@Test
	void testStartServiceStringStringDurationRunnable() {
		assertNotNull(jobKitEngine.startService(null, null, Duration.ZERO, task, disableTask));
	}

	@Test
	void testGetSpooler() {
		assertThrows(UnsupportedOperationException.class, () -> jobKitEngine.getSpooler());
	}

	@Test
	void testRunAllServicesOnce() {
		final var i = new AtomicInteger();
		task = () -> i.getAndIncrement();
		jobKitEngine.startService(null, null, 0, TimeUnit.DAYS, task, disableTask);
		jobKitEngine.runAllServicesOnce();
		assertEquals(1, i.get());
	}

	@Test
	void testIsEmptyActiveServicesList() {
		assertTrue(jobKitEngine.isEmptyActiveServicesList());

		var s = jobKitEngine.createService(null, null, task, disableTask);
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		s.enable();
		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		s.disable();
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		s = jobKitEngine.startService(null, null, 0, TimeUnit.DAYS, task, disableTask);
		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		s.disable();
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
	}

	@Test
	void testRunOneShot() {
		final var i = new AtomicInteger();
		final var j = new AtomicInteger();
		task = () -> i.getAndIncrement();
		final Consumer<Exception> afterRunCommand = e -> j.getAndIncrement();

		assertTrue(jobKitEngine.runOneShot(null, null, 0, task, afterRunCommand));
		assertEquals(1, i.get());
		assertEquals(1, j.get());
	}

	@Test
	void testRunOneShotJob() throws Exception {
		final var job = Mockito.mock(Job.class);
		assertTrue(jobKitEngine.runOneShot(job));

		verify(job, Mockito.times(1)).onJobStart();
		verify(job, Mockito.times(1)).onJobDone();
		verify(job, Mockito.times(1)).run();
		verify(job, Mockito.times(0)).onJobFail(ArgumentMatchers.any(Exception.class));
	}

	@Test
	void testRunOneShot_error() {
		final var i = new AtomicInteger();
		final var j = new AtomicInteger();
		final var runtimeException = new RuntimeException("A bad thing, but for test purpose only");

		task = () -> {
			i.getAndIncrement();
			throw runtimeException;
		};
		final var eR = new AtomicReference<Exception>();
		final Consumer<Exception> afterRunCommand = e -> {
			eR.set(e);
			j.getAndIncrement();
		};

		assertTrue(jobKitEngine.runOneShot(null, null, 0, task, afterRunCommand));
		assertEquals(1, i.get());
		assertEquals(1, j.get());
		assertEquals(runtimeException, eR.get());
	}

	@Test
	void testRunOneShotJob_error() throws Exception {
		final var job = Mockito.mock(Job.class);
		final var runtimeException = new RuntimeException("A bad thing, but for test purpose only");
		Mockito.doThrow(runtimeException).when(job).run();

		assertTrue(jobKitEngine.runOneShot(job));

		verify(job, Mockito.times(1)).onJobStart();
		verify(job, Mockito.times(0)).onJobDone();
		verify(job, Mockito.times(1)).run();
		verify(job, Mockito.times(1)).onJobFail(runtimeException);
	}

}
