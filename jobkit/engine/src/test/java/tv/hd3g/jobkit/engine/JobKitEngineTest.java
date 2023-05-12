package tv.hd3g.jobkit.engine;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class JobKitEngineTest {

	static Random random = new Random();

	@Mock
	ExecutionEvent executionEvent;
	@Mock
	BackgroundServiceEvent backgroundServiceEvent;
	@Mock
	RunnableWithException task;
	@Mock
	RunnableWithException disableTask;
	@Mock
	Consumer<Exception> afterRunCommand;

	String name;
	String spoolName;
	JobKitEngine jobKitEngine;
	Spooler spooler;
	ScheduledExecutorService scheduledExecutor;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = String.valueOf(random.nextLong());
		spoolName = String.valueOf(random.nextLong());
		scheduledExecutor = Executors.newScheduledThreadPool(1);
		jobKitEngine = new JobKitEngine(scheduledExecutor, executionEvent, backgroundServiceEvent);
		spooler = jobKitEngine.getSpooler();
	}

	@AfterEach
	void close() throws Exception {
		jobKitEngine.shutdown();
	}

	@Test
	void testRunOneShot() throws Exception {
		assertTrue(jobKitEngine.runOneShot(name, spoolName, 0, task, afterRunCommand));

		while (spooler.getRunningQueuesCount() > 0) {
			Thread.onSpinWait();
		}

		verify(task, times(1)).run();
		verify(afterRunCommand, times(1)).accept(isNull());
	}

	@Test
	void testCreateService() {
		final var s = jobKitEngine.createService(name, spoolName, task, disableTask);
		assertNotNull(s);
		assertFalse(s.isEnabled());
		assertEquals(0, s.getPriority());
		assertEquals(0, s.getTimedInterval(MILLISECONDS));
	}

	@Test
	void testStartServiceStringStringLongTimeUnitRunnable() throws Exception {
		final var i = new AtomicInteger();
		task = () -> i.getAndIncrement();

		final var s = jobKitEngine.startService(name, spoolName, 1, MILLISECONDS, task, disableTask);
		assertNotNull(s);
		assertTrue(s.isEnabled());
		assertEquals(0, s.getPriority());
		assertEquals(1, s.getTimedInterval(MILLISECONDS));

		CompletableFuture.runAsync(() -> {
			while (i.get() == 0) {
				Thread.onSpinWait();
			}
		}).get(1, TimeUnit.SECONDS);
	}

	@Test
	void testStartServiceStringStringDurationRunnable() throws Exception {
		final var i = new AtomicInteger();
		task = () -> i.getAndIncrement();

		final var s = jobKitEngine.startService(name, spoolName, Duration.ofMillis(1), task, disableTask);
		assertNotNull(s);
		assertTrue(s.isEnabled());
		assertEquals(0, s.getPriority());
		assertEquals(1, s.getTimedInterval(MILLISECONDS));

		CompletableFuture.runAsync(() -> {
			while (i.get() == 0) {
				Thread.onSpinWait();
			}
		}).get(1, TimeUnit.SECONDS);
	}

	@Test
	void testGetSpooler() {
		assertNotNull(jobKitEngine.getSpooler());
	}

	@Test
	void testShutdown_noMoreTasks() {
		jobKitEngine.shutdown();
		assertThrows(IllegalStateException.class,
				() -> jobKitEngine.runOneShot(name, spoolName, 0, task, afterRunCommand));
		verifyNoMoreInteractions(task, afterRunCommand);
	}

	@Test
	void testShutdown_stopService() throws Exception {
		final var smAfter = new CountDownLatch(1);
		final var supervisable = new AtomicReference<Supervisable>();

		final var service = jobKitEngine.startService(name, spoolName, 1, TimeUnit.DAYS, task,
				() -> {
					supervisable.set(Supervisable.getSupervisable());
					smAfter.countDown();
				});

		jobKitEngine.shutdown();

		assertFalse(service.isEnabled());
		assertFalse(service.isHasFirstStarted());
		assertTrue(smAfter.await(1, SECONDS));

		assertFalse(service.isEnabled());
		assertFalse(service.isHasFirstStarted());
		assertNotNull(supervisable.get());

		verifyNoMoreInteractions(task);
	}

	@Test
	void testShutdown_keepRun() throws Exception {
		final var spoolsNamesToKeepRunningToTheEnd = jobKitEngine.getSpoolsNamesToKeepRunningToTheEnd();
		assertNotNull(spoolsNamesToKeepRunningToTheEnd);
		assertTrue(spoolsNamesToKeepRunningToTheEnd.isEmpty());
		spoolsNamesToKeepRunningToTheEnd.add(spoolName + "A");

		final var total = 100;
		final var countA = new AtomicInteger(0);
		final var countB = new AtomicInteger(0);

		for (var pos = 0; pos < total; pos++) {
			assertTrue(jobKitEngine.runOneShot(name, spoolName + "A", 0,
					() -> {
						countA.getAndIncrement();
						Thread.sleep(1);// NOSONAR
					},
					afterRunCommand));
			assertTrue(jobKitEngine.runOneShot(name, spoolName + "B", 0,
					() -> {
						countB.getAndIncrement();
						Thread.sleep(1);// NOSONAR
					},
					afterRunCommand));
		}

		jobKitEngine.shutdown();

		assertEquals(total, countA.get());
		assertTrue(total > countB.get());
		verify(afterRunCommand, atLeast(total)).accept(isNull());
	}

	@Test
	void testOnApplicationReadyRunBackgroundServices_enabled() throws Exception {
		final var i = new AtomicInteger();
		task = () -> i.getAndIncrement();

		final var s = jobKitEngine.startService(name, spoolName, Duration.ofDays(1), task, disableTask);
		final var retry = s.getRetryAfterTimeFactor();

		jobKitEngine.onApplicationReadyRunBackgroundServices();
		assertEquals(1, s.getTimedInterval(DAYS));

		CompletableFuture.runAsync(() -> {
			while (i.get() == 0) {
				Thread.onSpinWait();
			}
		}).get(500, MILLISECONDS);

		assertEquals(1, i.get());
		assertEquals(retry, s.getRetryAfterTimeFactor());
		assertEquals(1, s.getTimedInterval(DAYS));
		assertTrue(s.isEnabled());
	}

	@Test
	void testOnApplicationReadyRunBackgroundServices_disabled() throws Exception {
		final var i = new AtomicInteger();
		task = () -> i.getAndIncrement();

		final var s = jobKitEngine.createService(name, spoolName, task, disableTask);

		jobKitEngine.onApplicationReadyRunBackgroundServices();

		Thread.sleep(10);// NOSONAR S2925

		assertEquals(0, i.get());
		assertFalse(s.isEnabled());
	}

}
