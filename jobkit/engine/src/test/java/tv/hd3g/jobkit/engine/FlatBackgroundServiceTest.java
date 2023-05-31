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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class FlatBackgroundServiceTest {
	static final Faker faker = Faker.instance();

	@Mock
	RunnableWithException endTask;
	@Mock
	Runnable endRun;

	FlatScheduledExecutorService scheduledExecutor;
	String spoolName;
	Runnable task;
	FlatBackgroundService flatBackgroundService;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		scheduledExecutor = new FlatScheduledExecutorService();
		task = () -> {
		};
		when(endTask.toRunnable()).thenReturn(endRun);
		spoolName = faker.numerify("spoolName###");
		flatBackgroundService = new FlatBackgroundService(scheduledExecutor, spoolName, task, endTask);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(endTask, endRun);
	}

	@Test
	void testEnable() {
		flatBackgroundService.enable();
		assertTrue(scheduledExecutor.contain(new FlatScheduledFuture(task)));
	}

	@Test
	void testDisable() {
		flatBackgroundService.enable();
		flatBackgroundService.disable();
		assertFalse(scheduledExecutor.contain(new FlatScheduledFuture(task)));
		verify(endTask, times(1)).toRunnable();
		verify(endRun, times(1)).run();
	}

	@Test
	void testIsEnabled() {
		assertFalse(flatBackgroundService.isEnabled());
		flatBackgroundService.enable();
		assertTrue(flatBackgroundService.isEnabled());
	}

	@Test
	void testSetTimedIntervalLongTimeUnit() {
		assertNotNull(flatBackgroundService.setTimedInterval(0, TimeUnit.DAYS));
	}

	@Test
	void testSetTimedIntervalDuration() {
		assertNotNull(flatBackgroundService.setTimedInterval(Duration.ZERO));
	}

	@Test
	void testSetRetryAfterTimeFactor() {
		assertNotNull(flatBackgroundService.setRetryAfterTimeFactor(0));
	}

	@Test
	void testGetSpoolName() {
		assertEquals(spoolName, flatBackgroundService.getSpoolName());
	}

}
