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
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class MaxSpoolQueueSizePolicyTest {
	static Faker faker = net.datafaker.Faker.instance();

	MaxSpoolQueueSizePolicy p;

	String spoolName;
	Duration checkTime;

	@Mock
	WatchableSpoolJobState activeJob;
	@Mock
	Set<WatchableSpoolJobState> queuedJobs;
	@Mock
	Set<WatchableBackgroundService> relativeBackgroundServices;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		p = new MaxSpoolQueueSizePolicy();
		spoolName = faker.numerify("spoolName###");
		checkTime = Duration.ofMillis(faker.random().nextLong(1, 100000));
		p.setCheckTime(checkTime);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_0maxsize() throws JobWatchdogPolicyWarning {
		p.setMaxSize(-1);
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_notThisSpool() throws JobWatchdogPolicyWarning {
		p.setOnlySpools(Set.of("onlythis"));
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk() throws JobWatchdogPolicyWarning {
		p.setMaxSize(5);
		when(queuedJobs.size()).thenReturn(1);
		assertEquals(checkTime, p.isStatusOk(spoolName, activeJob, queuedJobs).get());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verify(queuedJobs, atLeast(1)).size();
	}

	@Test
	void testIsStatusOk_noCheckTime() throws JobWatchdogPolicyWarning {
		p.setCheckTime(null);
		p.setMaxSize(5);
		when(queuedJobs.size()).thenReturn(1);
		assertEquals(MaxSpoolQueueSizePolicy.DEFAULT_CHECKTIME,
				p.isStatusOk(spoolName, activeJob, queuedJobs).get());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verify(queuedJobs, atLeast(1)).size();
	}

	@Test
	void testIsStatusOk_maxSize() throws JobWatchdogPolicyWarning {
		p.setMaxSize(5);
		when(queuedJobs.size()).thenReturn(6);
		assertThrows(JobWatchdogPolicyWarning.class,
				() -> p.isStatusOk(spoolName, activeJob, queuedJobs));
		assertThrows(JobWatchdogPolicyWarning.class,
				() -> p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices));
		verify(queuedJobs, atLeast(1)).size();
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
