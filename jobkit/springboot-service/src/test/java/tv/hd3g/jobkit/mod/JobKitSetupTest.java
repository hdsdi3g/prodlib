/*
 * This file is part of jobkit.
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
package tv.hd3g.jobkit.mod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.watchdog.LimitedExecTimePolicy;
import tv.hd3g.jobkit.engine.watchdog.LimitedServiceExecTimePolicy;
import tv.hd3g.jobkit.engine.watchdog.MaxSpoolQueueSizePolicy;

import tv.hd3g.jobkit.WithSupervisable;

@SpringBootTest
class JobKitSetupTest {

	@Autowired
	ScheduledExecutorService scheduledExecutorService;
	@Autowired
	JobKitEngine jobKitEngine;

	@Test
	void testGetScheduledExecutor() throws InterruptedException {
		final var latch = new CountDownLatch(1);
		assertDoesNotThrow(() -> scheduledExecutorService.execute(() -> latch.countDown()));
		latch.await(100, TimeUnit.MILLISECONDS);
	}

	@Test
	void testGetJobKitEngineWatchdog() {
		assertTrue(jobKitEngine.getJobKitWatchdog().getPolicies().isEmpty());
	}

	@SpringBootTest
	@TestPropertySource(locations = "classpath:application-watchdogpolicies.yml")
	static class Watchdog {

		@Autowired
		JobKitEngine jobKitEngine;
		@Autowired
		JobKitWatchdogConfig watchdogConfig;

		@BeforeEach
		void init() throws Exception {
			openMocks(this).close();
		}

		@AfterEach
		void end() {
		}

		@Test
		void testWatchdogPolicies() {
			final var p = jobKitEngine.getJobKitWatchdog().getPolicies();
			assertEquals(3, p.size());

			final var maxSpoolQueueSize = p.stream()
					.filter(f -> f instanceof MaxSpoolQueueSizePolicy)
					.map(f -> (MaxSpoolQueueSizePolicy) f)
					.findFirst()
					.get();
			assertEquals(10, maxSpoolQueueSize.getMaxSize());
			assertEquals(Set.of("AA"), maxSpoolQueueSize.getOnlySpools());

			final var limitedExecTime = p.stream()
					.filter(f -> f instanceof LimitedExecTimePolicy)
					.map(f -> (LimitedExecTimePolicy) f)
					.findFirst()
					.get();
			assertEquals(Duration.ofMillis(10000), limitedExecTime.getMaxExecTime());
			assertEquals(Set.of("BB"), limitedExecTime.getOnlySpools());

			final var limitedServiceExecTimePolicy = p.stream()
					.filter(f -> f instanceof LimitedServiceExecTimePolicy)
					.map(f -> (LimitedServiceExecTimePolicy) f)
					.findFirst()
					.get();
			assertEquals(5, limitedServiceExecTimePolicy.getWaitFactor());
			assertEquals(Set.of("CC"), limitedServiceExecTimePolicy.getOnlySpools());
		}

	}

	void shouldSupervisableAspectRuntimeHints() {
		final var hints = new RuntimeHints();
		final var expectSupervisable = RuntimeHintsPredicates.reflection().onType(WithSupervisable.class);
		new JobKitSetup.SupervisableAspectRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(expectSupervisable).accepts(hints);
	}
}
