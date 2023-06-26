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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.engine.watchdog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import net.datafaker.Faker;

class LimitedExecTimePolicyTest {
	static Faker faker = net.datafaker.Faker.instance();

	LimitedExecTimePolicy p;

	String spoolName;
	Duration maxExecTime;

	@Mock
	WatchableSpoolJobState activeJob;
	@Mock
	Set<WatchableSpoolJobState> queuedJobs;
	@Mock
	Set<WatchableBackgroundService> relativeBackgroundServices;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		p = new LimitedExecTimePolicy();
		spoolName = faker.numerify("spoolName###");
		maxExecTime = Duration.ofMillis(faker.random().nextLong(1, 100000));
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(activeJob, queuedJobs, relativeBackgroundServices);
	}

	private static Stream<Arguments> testIsStatusOk_noMaxExectime() {
		return Stream.of(
				Arguments.of((Duration) null),
				Arguments.of(Duration.ZERO),
				Arguments.of(Duration.ofMillis(-1)));
	}

	@ParameterizedTest
	@MethodSource
	void testIsStatusOk_noMaxExectime(final Duration maxExecTime) throws JobWatchdogPolicyWarning {
		p.setMaxExecTime(maxExecTime);
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_notThisSpool() throws JobWatchdogPolicyWarning {
		p.setMaxExecTime(maxExecTime);
		p.setOnlySpools(Set.of("onlythis"));
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_noRun() throws JobWatchdogPolicyWarning {
		p.setMaxExecTime(maxExecTime);
		when(activeJob.getRunTime()).thenReturn(Optional.empty());
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verify(activeJob, atLeast(1)).getRunTime();
	}

	@Test
	void testIsStatusOk_runOk() throws JobWatchdogPolicyWarning {
		final var delta = faker.random().nextLong(1, maxExecTime.toMillis() - 1);
		p.setMaxExecTime(maxExecTime);
		when(activeJob.getRunTime()).thenReturn(Optional.ofNullable(Duration.ofMillis(maxExecTime.toMillis() - delta)));

		assertEquals(Duration.ofMillis(delta), p.isStatusOk(spoolName, activeJob, queuedJobs).get());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);

		verify(activeJob, atLeast(1)).getRunTime();
	}

	@Test
	void testIsStatusOk_runNok() throws JobWatchdogPolicyWarning {
		final var delta = faker.random().nextLong(maxExecTime.toMillis() + 1, maxExecTime.toMillis() * 1000);
		p.setMaxExecTime(maxExecTime);
		when(activeJob.getRunTime()).thenReturn(Optional.ofNullable(Duration.ofMillis(maxExecTime.toMillis() + delta)));

		assertThrows(JobWatchdogPolicyWarning.class,
				() -> p.isStatusOk(spoolName, activeJob, queuedJobs));
		assertThrows(JobWatchdogPolicyWarning.class,
				() -> p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices));

		verify(activeJob, atLeast(1)).getRunTime();
	}

	@Test
	void testGetDescription() {
		assertFalse(p.getDescription().isEmpty());
		p.setNotSpools(Set.of("Something"));
		assertFalse(p.getDescription().isEmpty());
		p.setOnlySpools(Set.of("Something"));
		assertFalse(p.getDescription().isEmpty());
	}

}
