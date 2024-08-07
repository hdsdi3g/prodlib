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
package tv.hd3g.jobkit.engine;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicy;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicyWarning;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogSpoolReport;
import tv.hd3g.jobkit.engine.watchdog.WatchableBackgroundService;
import tv.hd3g.jobkit.engine.watchdog.WatchableSpoolJobState;

class JobKitWatchdogTest {

	private static Faker faker = net.datafaker.Faker.instance();

	JobKitWatchdog w;

	String serviceNameLow;
	String serviceName;
	String serviceNameHigh;
	String spoolName;
	long timedInterval;
	String commandName;
	long startedDate;
	long createdIndex;
	long durationToQueue;

	@Mock
	ScheduledExecutorService sch;
	@Mock
	ScheduledFuture<Object> scheduledFuture;
	@Mock
	SupervisableEvents supervisableEvents;
	@Mock
	JobWatchdogPolicy policy;
	@Mock
	WatchableSpoolJob job;
	@Mock
	WatchableSpoolJob waitJob;
	@Mock
	SpoolExecutor spoolExecutor;
	@Mock
	Optional<StackTraceElement> creator;
	@Captor
	ArgumentCaptor<Runnable> run;
	@Captor
	ArgumentCaptor<WatchableSpoolJobState> activeJobCaptor;
	@Captor
	ArgumentCaptor<Set<WatchableSpoolJobState>> queuedJobsCaptor;
	@Captor
	ArgumentCaptor<Set<WatchableBackgroundService>> relativeBackgroundServicesCaptor;
	@Captor
	ArgumentCaptor<JobWatchdogSpoolReport> reportCaptor;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		w = new JobKitWatchdog(supervisableEvents, sch);
		serviceNameLow = faker.numerify("serviceNameLow###");
		serviceName = faker.numerify("serviceName###");
		serviceNameHigh = faker.numerify("serviceNameHigh###");
		spoolName = faker.numerify("spoolName###");
		timedInterval = faker.random().nextLong(1000, 10000000);
		durationToQueue = faker.random().nextLong(1000, 10000000);

		when(policy.getDescription()).thenReturn(faker.numerify("description###"));

		when(sch.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(
				(ScheduledFuture) scheduledFuture);

		commandName = faker.numerify("commandName###");
		createdIndex = faker.random().nextLong();
		startedDate = System.currentTimeMillis();

		when(job.getCommandName()).thenReturn(commandName);
		when(job.getSpoolName()).thenReturn(spoolName);
		when(job.getCreatedIndex()).thenReturn(createdIndex);
		when(job.getCreator()).thenReturn(creator);
		when(job.getExecutorReferer()).thenReturn(spoolExecutor);

		when(waitJob.getCommandName()).thenReturn(commandName);
		when(waitJob.getSpoolName()).thenReturn(spoolName);
		when(waitJob.getCreatedIndex()).thenReturn(createdIndex + 1l);
		when(waitJob.getCreator()).thenReturn(creator);
		when(waitJob.getExecutorReferer()).thenReturn(spoolExecutor);
	}

	@AfterEach
	void end() {
		verify(policy, atLeast(0)).getDescription();
		verify(scheduledFuture, atLeast(0)).isDone();
		verify(scheduledFuture, atLeast(0)).isCancelled();
		verify(scheduledFuture, atLeast(0)).getDelay(MILLISECONDS);
		verify(sch, atLeast(0)).isShutdown();
		verify(sch, atLeast(0)).isTerminated();

		verifyNoMoreInteractions(sch, scheduledFuture, supervisableEvents, policy, spoolExecutor, creator);
	}

	@Test
	void testAddGetPolicies() {
		final var actual = w.getPolicies();
		assertEquals(List.of(), actual);
		assertThrows(UnsupportedOperationException.class, () -> actual.add(policy));
		w.addPolicies(policy);
		assertTrue(w.getPolicies().contains(policy));
		assertFalse(actual.contains(policy));
	}

	@Nested
	class OkPolicyOneShot {

		@BeforeEach
		void init() throws Exception {
			w.addPolicies(policy);
		}

		@ParameterizedTest
		@ValueSource(booleans = { true, false })
		void testRefreshBackgroundService(final boolean enabled) {
			w.refreshBackgroundService(serviceName, spoolName, enabled, timedInterval);
			verify(sch, times(1)).execute(run.capture());
			run.getValue().run();
		}

		@Test
		void testRefreshBackgroundService_add_enabled() {
			w.refreshBackgroundService(serviceNameLow, spoolName, true, timedInterval / 2l);
			w.refreshBackgroundService(serviceNameHigh, spoolName, true, timedInterval * 2l);
			w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);

			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);
		}

		@Test
		void testRefreshBackgroundService_addmultiple_enabled() {
			w.refreshBackgroundService(serviceNameLow, spoolName, true, timedInterval / 2l);
			w.refreshBackgroundService(serviceNameHigh, spoolName, true, timedInterval * 2l);
			w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);
			w.refreshBackgroundService(serviceNameLow, spoolName, true, timedInterval / 2l);
			w.refreshBackgroundService(serviceNameHigh, spoolName, false, timedInterval * 2l);
			w.refreshBackgroundService(serviceNameHigh, spoolName, true, timedInterval * 2l);

			verify(sch, times(6)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);
		}

		@Test
		void testAddJob() {
			w.addJob(job);
			verify(sch, times(1)).execute(run.capture());
			run.getValue().run();
		}

		@Test
		void testAddStartJob() throws JobWatchdogPolicyWarning {
			w.addJob(job);
			w.addJob(waitJob);
			w.startJob(job, startedDate);
			verify(sch, times(3)).execute(run.capture());
			run.getValue().run();
			verify(policy, times(1)).isStatusOk(eq(spoolName), activeJobCaptor.capture(), queuedJobsCaptor.capture());

			final var activeJob = activeJobCaptor.getValue();
			assertEquals(commandName, activeJob.commandName());
			assertTrue(activeJob.createdDate().getTime() <= startedDate + 100);
			assertEquals(createdIndex, activeJob.createdIndex());
			assertTrue(activeJob.getRunTime().isPresent());
			assertEquals(startedDate, activeJob.startedDate().get());
			assertEquals(creator, activeJob.creator());

			final var queuedJobs = queuedJobsCaptor.getValue();
			assertFalse(queuedJobs.isEmpty());
			final var queuedJob = queuedJobs.stream().findFirst().get();
			assertEquals(commandName, queuedJob.commandName());
			assertTrue(queuedJob.createdDate().getTime() <= startedDate + 100);
			assertEquals(createdIndex + 1, queuedJob.createdIndex());
			assertFalse(queuedJob.getRunTime().isPresent());
			assertFalse(queuedJob.startedDate().isPresent());
			assertEquals(creator, queuedJob.creator());
		}

		@Test
		void testAddStartEndJob() throws JobWatchdogPolicyWarning {
			w.addJob(job);
			w.startJob(job, startedDate);
			w.endJob(job);

			verify(sch, times(3)).execute(run.capture());
			run.getValue().run();
		}

	}

	@Nested
	class OkPolicyRegular extends OkPolicyOneShot {

		@Override
		@BeforeEach
		void init() throws Exception {
			super.init();
			when(policy.isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), anySet()))
					.thenReturn(Optional.ofNullable(Duration.ofMillis(durationToQueue)));
		}

		@Override
		@Test
		void testAddStartJob() throws JobWatchdogPolicyWarning {
			super.testAddStartJob();
			verify(sch, times(1)).schedule(any(Runnable.class), eq(durationToQueue), eq(MILLISECONDS));
		}

		@Test
		void testAddStartEndJob_notLowerDurationToQueue() throws JobWatchdogPolicyWarning {
			when(scheduledFuture.isDone()).thenReturn(true);
			when(scheduledFuture.getDelay(MILLISECONDS)).thenReturn(durationToQueue * 2l);

			w.addJob(job);
			verify(sch, times(1)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(waitJob);
			verify(sch, times(2)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			when(scheduledFuture.isDone()).thenReturn(false);

			w.startJob(job, startedDate);
			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(scheduledFuture, times(5)).cancel(false);

			w.endJob(job);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(policy, times(6)).isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), argThat(s -> s
					.size() == 1));
			verify(sch, times(6)).schedule(any(Runnable.class), eq(durationToQueue), eq(MILLISECONDS));
		}

		@Test
		void testAddStartEndJob_lowerDurationToQueue() throws JobWatchdogPolicyWarning {
			when(scheduledFuture.isDone()).thenReturn(false);
			when(scheduledFuture.getDelay(MILLISECONDS)).thenReturn(durationToQueue / 2l);

			w.addJob(job);
			verify(sch, times(1)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(waitJob);
			verify(sch, times(2)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.startJob(job, startedDate);
			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.endJob(job);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(policy, times(6)).isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), argThat(s -> s
					.size() == 1));
			verify(sch, times(1)).schedule(any(Runnable.class), eq(durationToQueue), eq(MILLISECONDS));
		}

		@Test
		void testAddStartEndJob_Service() throws JobWatchdogPolicyWarning {
			w.refreshBackgroundService(serviceNameLow, spoolName, true, timedInterval / 2l);
			w.refreshBackgroundService(serviceNameHigh, spoolName, true, timedInterval * 2l);
			w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);

			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(job);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(waitJob);
			verify(sch, times(5)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.startJob(job, startedDate);
			verify(sch, times(6)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.endJob(job);
			verify(sch, times(7)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(sch, times(1)).schedule(any(Runnable.class), eq(timedInterval / 2), eq(MILLISECONDS));
			verify(policy, times(18))
					.isStatusOk(
							eq(spoolName),
							any(WatchableSpoolJobState.class),
							argThat(s -> s.size() == 1),
							argThat(s -> s.size() == 3));
		}

		@Test
		void testShutdown() throws JobWatchdogPolicyWarning {
			super.testAddStartJob();
			w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(sch, times(1)).schedule(any(Runnable.class), eq(durationToQueue), eq(MILLISECONDS));

			w.shutdown();
			verify(scheduledFuture, times(1)).cancel(true);

			verify(policy, times(7)).isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), anySet(), anySet());
		}

	}

	@Test
	void testShutdown_notRun() {
		w.shutdown();
		w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);
		w.addJob(job);
		w.addJob(waitJob);
		w.startJob(job, startedDate);
		w.endJob(job);

		verify(sch, times(0)).execute(any());

		w.shutdown();
	}

	@Nested
	class KoPolicy {

		JobWatchdogPolicyWarning warning;
		JobWatchdogSpoolReport report;
		AtomicInteger count;

		@BeforeEach
		void init() throws Exception {
			w.addPolicies(policy);
			warning = new JobWatchdogPolicyWarning(faker.numerify("warningMessage###"));
			count = new AtomicInteger(0);
		}

		@Test
		void testAddStartReleaseEndJob() throws JobWatchdogPolicyWarning {
			final var now = System.currentTimeMillis();
			when(policy.isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), anySet()))
					.thenAnswer(invocation -> {
						if (count.getAndAdd(1) <= 1) {
							throw warning;
						}
						return Optional.ofNullable(Duration.ofMillis(durationToQueue));
					});

			w.addJob(job);
			verify(sch, times(1)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(waitJob);
			verify(sch, times(2)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.startJob(job, startedDate);
			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.endJob(job);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(policy, times(6)).isStatusOk(
					eq(spoolName), any(WatchableSpoolJobState.class), argThat(s -> s.size() == 1));
			verify(sch, times(1)).schedule(any(Runnable.class), eq(durationToQueue), eq(MILLISECONDS));

			verify(supervisableEvents, times(1)).onJobWatchdogSpoolReport(reportCaptor.capture());
			final var report = reportCaptor.getValue();

			assertEquals(commandName, report.activeJob().commandName());
			assertEquals(createdIndex, report.activeJob().createdIndex());
			assertEquals(startedDate, report.activeJob().startedDate().get());
			assertEquals(creator, report.activeJob().creator());
			assertTrue(now <= report.activeJob().createdDate().getTime());
			assertTrue(System.currentTimeMillis() >= report.activeJob().createdDate().getTime());
			assertEquals(spoolName, report.spoolName());
			assertTrue(report.relativeBackgroundServices().isEmpty());
			assertEquals(policy, report.policy());
			assertTrue(now <= report.created().getTime());
			assertTrue(System.currentTimeMillis() >= report.created().getTime());
			assertTrue(System.currentTimeMillis() - startedDate <= report.activeJob().getRunTime().get().toMillis());
			assertEquals(warning, report.warning());

			assertFalse(report.queuedJobs().isEmpty());
			final var queued = report.queuedJobs().stream().findFirst().get();
			assertTrue(queued.getRunTime().isEmpty());
			assertTrue(queued.startedDate().isEmpty());
			assertEquals(createdIndex + 1, queued.createdIndex());

			verify(supervisableEvents, times(1)).onJobWatchdogSpoolReleaseReport(reportCaptor.capture());
			assertEquals(report, reportCaptor.getValue());
		}

		@Test
		void testAddStartReleaseEndJob_service() throws JobWatchdogPolicyWarning {
			final var now = System.currentTimeMillis();
			doAnswer(invocation -> {
				if (count.getAndAdd(1) <= 1) {
					throw warning;
				}
				return null;
			}).when(policy).isStatusOk(eq(spoolName), any(WatchableSpoolJobState.class), anySet(), anySet());
			when(scheduledFuture.isDone()).thenReturn(true);

			w.refreshBackgroundService(serviceName, spoolName, true, timedInterval);
			verify(sch, times(1)).execute(run.capture());

			w.addJob(job);
			verify(sch, times(2)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.addJob(waitJob);
			verify(sch, times(3)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.startJob(job, startedDate);
			verify(sch, times(4)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			w.endJob(job);
			verify(sch, times(5)).execute(run.capture());
			run.getAllValues().forEach(Runnable::run);

			verify(policy, times(10)).isStatusOk(
					eq(spoolName),
					any(WatchableSpoolJobState.class),
					argThat(s -> s.size() == 1),
					argThat(s -> s.size() == 1));
			verify(sch, times(8)).schedule(any(Runnable.class), eq(timedInterval), eq(MILLISECONDS));

			verify(supervisableEvents, times(1)).onJobWatchdogSpoolReport(reportCaptor.capture());
			final var report = reportCaptor.getValue();

			assertEquals(commandName, report.activeJob().commandName());
			assertEquals(createdIndex, report.activeJob().createdIndex());
			assertEquals(startedDate, report.activeJob().startedDate().get());
			assertEquals(creator, report.activeJob().creator());
			assertTrue(now <= report.activeJob().createdDate().getTime());
			assertTrue(System.currentTimeMillis() >= report.activeJob().createdDate().getTime());
			assertEquals(spoolName, report.spoolName());

			assertEquals(1, report.relativeBackgroundServices().size());
			final var service = report.relativeBackgroundServices().stream().findFirst().get();
			assertEquals(serviceName, service.serviceName());
			assertEquals(spoolName, service.spoolName());
			assertEquals(timedInterval, service.timedInterval());

			assertEquals(policy, report.policy());
			assertTrue(now <= report.created().getTime());
			assertTrue(System.currentTimeMillis() >= report.created().getTime());
			assertTrue(System.currentTimeMillis() - startedDate <= report.activeJob().getRunTime().get().toMillis());
			assertEquals(warning, report.warning());

			assertFalse(report.queuedJobs().isEmpty());
			final var queued = report.queuedJobs().stream().findFirst().get();
			assertTrue(queued.getRunTime().isEmpty());
			assertTrue(queued.startedDate().isEmpty());
			assertEquals(createdIndex + 1, queued.createdIndex());

			verify(supervisableEvents, times(1)).onJobWatchdogSpoolReleaseReport(reportCaptor.capture());
			assertEquals(report, reportCaptor.getValue());
		}
	}

}
