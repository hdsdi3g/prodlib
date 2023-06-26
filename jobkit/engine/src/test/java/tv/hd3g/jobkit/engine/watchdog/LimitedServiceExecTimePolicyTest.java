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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.Mock;

import net.datafaker.Faker;

class LimitedServiceExecTimePolicyTest {

	static Faker faker = net.datafaker.Faker.instance();

	LimitedServiceExecTimePolicy p;

	String spoolName;
	int waitFactor;
	long timedInterval;
	WatchableBackgroundService currentService;
	WatchableBackgroundService shorterTimeService;

	@Mock
	WatchableSpoolJobState activeJob;
	@Mock
	Set<WatchableSpoolJobState> queuedJobs;
	@Mock
	Set<WatchableBackgroundService> relativeBackgroundServices;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		p = new LimitedServiceExecTimePolicy();
		spoolName = faker.numerify("spoolName###");
		waitFactor = faker.random().nextInt(1, 10);
		timedInterval = faker.random().nextLong(1, 100000);

		currentService = new WatchableBackgroundService(faker.numerify("serviceName###"), spoolName, timedInterval);
		shorterTimeService = new WatchableBackgroundService(
				faker.numerify("serviceName###"), spoolName, timedInterval / 2);
		when(relativeBackgroundServices.stream()).thenReturn(Stream.of(shorterTimeService, currentService));
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_noWaitFactor() throws JobWatchdogPolicyWarning {
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		p.setWaitFactor(faker.random().nextInt(-100000, -1));
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verifyNoInteractions(activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_notThisSpool() throws JobWatchdogPolicyWarning {
		p.setWaitFactor(waitFactor);
		p.setOnlySpools(Set.of("onlythis"));
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verifyNoInteractions(activeJob, queuedJobs, relativeBackgroundServices);
	}

	@Test
	void testIsStatusOk_noRun() throws JobWatchdogPolicyWarning {
		p.setWaitFactor(waitFactor);
		when(activeJob.getRunTime()).thenReturn(Optional.empty());
		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verify(activeJob, atLeast(1)).getRunTime();
	}

	@Test
	void testIsStatusOk_runOk() throws JobWatchdogPolicyWarning {
		p.setWaitFactor(waitFactor);
		when(activeJob.getRunTime()).thenReturn(Optional.ofNullable(Duration.ofMillis(timedInterval - 1)));

		p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices);
		verify(activeJob, atLeast(1)).getRunTime();
		verify(relativeBackgroundServices, atLeast(1)).stream();
	}

	@Test
	void testIsStatusOk_runNok() throws JobWatchdogPolicyWarning {
		p.setWaitFactor(waitFactor);
		when(activeJob.getRunTime())
				.thenReturn(Optional.ofNullable(Duration.ofMillis(timedInterval * waitFactor + 2l)));

		assertThrows(JobWatchdogPolicyWarning.class,
				() -> p.isStatusOk(spoolName, activeJob, queuedJobs, relativeBackgroundServices));
		verify(activeJob, atLeast(1)).getRunTime();
		verify(relativeBackgroundServices, atLeast(1)).stream();
	}

	@Test
	void testGetDescription() {
		assertFalse(p.getDescription().isEmpty());
		p.setNotSpools(Set.of("Something"));
		assertFalse(p.getDescription().isEmpty());
		p.setOnlySpools(Set.of("Something"));
		assertFalse(p.getDescription().isEmpty());
	}

	@Test
	void testIsStatusOk_notService() throws JobWatchdogPolicyWarning {
		p.setWaitFactor(waitFactor);
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
		p.setWaitFactor(faker.random().nextInt());
		assertTrue(p.isStatusOk(spoolName, activeJob, queuedJobs).isEmpty());
	}

}
