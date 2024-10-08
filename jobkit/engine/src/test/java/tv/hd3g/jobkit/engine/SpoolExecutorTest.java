package tv.hd3g.jobkit.engine;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import net.datafaker.Faker;

class SpoolExecutorTest {

	static Random random = new Random();

	@Mock
	ExecutionEvent event;
	@Mock
	SupervisableEvents sEvent;
	@Mock
	JobKitWatchdog jobKitWatchdog;
	@Captor
	ArgumentCaptor<Optional<Exception>> oExceptionCaptor;

	String name;
	String spoolExecutorName;
	String threadName;
	SpoolExecutor spoolExecutor;
	AtomicLong threadCount;
	List<Integer> runnedTasks;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = "InternalTest " + String.valueOf(System.nanoTime());
		runnedTasks = Collections.synchronizedList(new ArrayList<>());

		spoolExecutorName = "Internal test Spool executor";
		threadName = "SpoolExecutor #0";
		threadCount = new AtomicLong();
		spoolExecutor = new SpoolExecutor(spoolExecutorName, event, threadCount, sEvent, jobKitWatchdog);
	}

	@AfterEach
	void ends() {
		for (var pos = 0; pos < runnedTasks.size(); pos++) {
			assertEquals(pos, runnedTasks.get(pos));
		}
	}

	@Test
	void testAddToQueue() throws InterruptedException {
		final var smChkVerifyEvent = new CountDownLatch(1);
		final var smCmd = new CountDownLatch(1);
		final var smAfter = new CountDownLatch(1);

		assertTrue(spoolExecutor.addToQueue(() -> {
			try {
				smChkVerifyEvent.await(10, SECONDS);
			} catch (final InterruptedException e1) {
				throw new IllegalStateException(e1);
			}
			smCmd.countDown();
		}, name, 0, e -> {
			smAfter.countDown();
		}));

		Thread.sleep(2);// NOSONAR
		verify(event, times(1)).beforeStart(
				eq(name),
				longThat(m -> m < System.currentTimeMillis()),
				eq(spoolExecutor));
		verifyTotalAfterRunCorrectly(0);
		verifyTotalAfterFailedRun(0);
		smChkVerifyEvent.countDown();

		assertTrue(smCmd.await(10, SECONDS));

		Thread.sleep(2);// NOSONAR
		verify(event, times(1)).afterRunCorrectly(eq(name),
				longThat(m -> m < System.currentTimeMillis()),
				longThat(m -> m < 100l),
				eq(spoolExecutor));
		verifyTotalAfterFailedRun(0);

		assertTrue(smAfter.await(10, SECONDS));

		verifyTotalBeforeStart(1);
		verifyTotalAfterRunCorrectly(1);
		verifyTotalAfterFailedRun(0);
		verify(event, times(0)).shutdownSpooler(any(Supervisable.class));

		checkSupervisableEventOnEnd(3, true);

		checkWatchdog(1);
	}

	@Test
	void testAddToQueue_withError() throws InterruptedException {
		final var total = 5;

		for (var pos = 0; pos < total; pos++) {
			final var smAfter = new CountDownLatch(1);
			final var captured = new AtomicReference<Exception>();
			final var _pos = pos;
			assertTrue(spoolExecutor.addToQueue(() -> {
				throw new IllegalArgumentException(String.valueOf(_pos));
			}, name, 0, e -> {
				captured.set(e);
				smAfter.countDown();
			}));
			assertTrue(smAfter.await(10, SECONDS));
			assertNotNull(captured.get());
			assertEquals(String.valueOf(_pos), captured.get().getMessage());
		}

		Thread.sleep(2);// NOSONAR
		verify(event, times(total)).beforeStart(
				eq(name),
				longThat(m -> m < System.currentTimeMillis()),
				eq(spoolExecutor));
		verifyTotalAfterRunCorrectly(0);
		verifyTotalAfterFailedRun(total);
		verify(event, times(total)).afterFailedRun(
				eq(name),
				longThat(m -> m < System.currentTimeMillis()),
				longThat(m -> m < 100l),
				eq(spoolExecutor),
				any(IllegalArgumentException.class));

		verify(event, times(0)).shutdownSpooler(any(Supervisable.class));
		verify(sEvent, times(total * 4)).onEnd(any(), any());

		checkWatchdog(total);
	}

	@Test
	void testAddToQueue_multiple() throws InterruptedException {
		final var total = 50;

		final var smBefore = new CountDownLatch(total);
		final var smAfter = new CountDownLatch(total);
		final var count = new AtomicInteger(0);

		for (var pos = 0; pos < total; pos++) {
			final var val = pos;
			assertTrue(spoolExecutor.addToQueue(() -> {
				count.incrementAndGet();
				runnedTasks.add(val);
				smBefore.countDown();
			}, name, 0, e -> {
				smAfter.countDown();
			}));
		}

		assertTrue(spoolExecutor.isRunning());
		assertTrue(smBefore.await(20, SECONDS));
		assertTrue(smAfter.await(20, SECONDS));
		assertEquals(total, count.get());
		assertEquals(0, spoolExecutor.getQueueSize());
		assertEquals(total, runnedTasks.size());

		while (spoolExecutor.isRunning()) {
			Thread.onSpinWait();
		}

		checkSupervisableEventOnEnd(total * 3, true);

		checkWatchdog(total);
	}

	@Test
	void testAddToQueue_onebyone() throws InterruptedException {
		final var total = 10;
		final var count = new AtomicInteger(0);

		for (var pos = 0; pos < total; pos++) {
			final var val = pos;
			final var smAfter = new CountDownLatch(1);
			assertTrue(spoolExecutor.addToQueue(() -> {
				count.incrementAndGet();
				runnedTasks.add(val);
			}, name, 0, e -> {
				smAfter.countDown();
			}));
			assertTrue(smAfter.await(10, SECONDS));

			assertEquals(0, spoolExecutor.getQueueSize());
			while (spoolExecutor.isRunning()) {
				Thread.onSpinWait();
			}
		}
		assertEquals(total, count.get());

		checkSupervisableEventOnEnd(total * 3, true);

		checkWatchdog(total);
	}

	@Test
	void testAddToQueue_afterRunError() throws InterruptedException {
		final var smCmd0 = new CountDownLatch(1);

		assertTrue(spoolExecutor.addToQueue(() -> {
			smCmd0.countDown();
		}, name, 0, e -> {
			throw new IllegalArgumentException("This is a test error, this is normal if you see it in log message...");
		}));
		assertTrue(smCmd0.await(10, SECONDS));

		final var smCmd1 = new CountDownLatch(2);
		assertTrue(spoolExecutor.addToQueue(() -> {
			smCmd1.countDown();
		}, name, 0, e -> {
			smCmd1.countDown();
		}));
		assertTrue(smCmd1.await(10, SECONDS));

		assertEquals(0, spoolExecutor.getQueueSize());
		while (spoolExecutor.isRunning()) {
			Thread.onSpinWait();
		}

		verify(sEvent, atLeast(7)).onEnd(any(), any());

		checkWatchdog(2);
	}

	@Test
	void testGetQueueSize() throws InterruptedException {
		assertEquals(0, spoolExecutor.getQueueSize());

		final var smCmd0 = new CountDownLatch(1);
		final var smCmd1 = new CountDownLatch(1);
		final var reverseCmd = new CountDownLatch(1);
		spoolExecutor.addToQueue(() -> {
			smCmd0.countDown();
			reverseCmd.await(10, SECONDS);
		}, name, 0, e -> {
			smCmd1.countDown();
		});

		assertTrue(smCmd0.await(10, SECONDS));
		spoolExecutor.addToQueue(() -> {
		}, name, 0, e -> {
		});
		assertEquals(1, spoolExecutor.getQueueSize());
		reverseCmd.countDown();

		assertTrue(smCmd1.await(10, SECONDS));
		Thread.sleep(50);// NOSONAR
		assertEquals(0, spoolExecutor.getQueueSize());

		verify(event, times(0)).shutdownSpooler(any(Supervisable.class));

		checkSupervisableEventOnEnd(7, true);

		checkWatchdog(2);
	}

	@Test
	void testIsRunning() throws InterruptedException {
		assertFalse(spoolExecutor.isRunning());

		final var smCmd0 = new CountDownLatch(1);
		final var smCmd1 = new CountDownLatch(1);
		final var reverseCmd = new CountDownLatch(1);
		spoolExecutor.addToQueue(() -> {
			smCmd0.countDown();
			reverseCmd.await(10, SECONDS);
		}, name, 0, e -> {
			smCmd1.countDown();
		});
		assertTrue(smCmd0.await(10, SECONDS));
		assertTrue(spoolExecutor.isRunning());
		reverseCmd.countDown();
		assertTrue(smCmd1.await(10, SECONDS));
		Thread.sleep(50);// NOSONAR
		assertFalse(spoolExecutor.isRunning());

		checkSupervisableEventOnEnd(3, true);

		checkWatchdog(1);
	}

	@Test
	void testStopToAcceptNewJobs() throws InterruptedException {
		spoolExecutor.stopToAcceptNewJobs();
		final var count = new AtomicInteger(0);
		assertFalse(spoolExecutor.addToQueue(() -> {
			count.getAndIncrement();
		}, name, 0, e -> {
			count.getAndIncrement();
		}));

		Thread.sleep(10);// NOSONAR
		assertEquals(0, count.get());
	}

	@Test
	void testClean_purgeWaitList_noEmpty() {
		final var total = 1000;
		final var count = new AtomicInteger(0);

		for (var pos = 0; pos < total; pos++) {
			assertTrue(spoolExecutor.addToQueue(() -> {
				count.incrementAndGet();
				Thread.sleep(10);// NOSONAR
			}, name, 0, e -> {
				count.incrementAndGet();
			}));
		}

		spoolExecutor.clean(true);
		assertTrue(count.get() / 2 < total);
		verify(sEvent, atLeast(1)).onEnd(any(), any());

		checkWatchdog(atMost(1000));
	}

	@Test
	void testClean_purgeWaitList_empty() {
		spoolExecutor.clean(true);
		verify(sEvent, times(0)).onEnd(any(), any());
	}

	@Test
	void testClean_notPurgeWaitList_empty() {
		spoolExecutor.clean(false);
		verify(sEvent, times(0)).onEnd(any(), any());
	}

	@Test
	void testClean_notPurgeWaitList() throws InterruptedException {
		final var total = 100;
		final var smCmd = new CountDownLatch(total * 2);

		for (var pos = 0; pos < total; pos++) {
			assertTrue(spoolExecutor.addToQueue(() -> {
				smCmd.countDown();
				Thread.sleep(1);// NOSONAR
			}, name, 0, e -> {
				smCmd.countDown();
			}));
		}

		spoolExecutor.clean(false);

		assertTrue(smCmd.await(total * 10, SECONDS));
		assertEquals(0, spoolExecutor.getQueueSize());
		while (spoolExecutor.isRunning()) {
			Thread.onSpinWait();
		}

		checkSupervisableEventOnEnd(total * 4 - 4, true);

		verify(jobKitWatchdog, times(total)).addJob(any(WatchableSpoolJob.class));
		verify(jobKitWatchdog, times(total)).startJob(any(WatchableSpoolJob.class), anyLong());
		verify(jobKitWatchdog, times(total)).endJob(any(WatchableSpoolJob.class));
	}

	@Test
	void testPriorities() throws InterruptedException {
		final var startDate = System.currentTimeMillis();
		final var count = 3;

		final var latchCheck = new CountDownLatch(count);
		final var latchFeed = new CountDownLatch(1);

		class PJob {
			volatile long startTime;
			final RunnableWithException command = () -> {
				startTime = System.currentTimeMillis() - startDate;
				try {
					Thread.sleep(2);// NOSONAR
					latchFeed.await(10, SECONDS);
				} catch (final InterruptedException e) {// NOSONAR
				}
				latchCheck.countDown();
			};
			final String name = "N" /*+ String.valueOf(random.nextInt(10000))*/;
			final int priority = random.nextInt(count * 100);
			final Consumer<Exception> afterRunCommand = e -> {
			};
		}

		final var allPjobs = IntStream.range(0, count)
				.mapToObj(i -> new PJob())
				.toList();
		allPjobs.forEach(j -> spoolExecutor.addToQueue(j.command, j.name, j.priority, j.afterRunCommand));

		latchFeed.countDown();
		assertTrue(latchCheck.await(10, SECONDS));

		/**
		 * skip 1 because the first pushed will be always random.
		 * During the first sleep, the others will be added, and correcly sorted.
		 */
		final var dateSort = allPjobs.stream()
				.skip(1)
				.sorted((l, r) -> Long.compare(l.startTime, r.startTime))
				.map(j -> j.name + "_t" + j.startTime + "_P" + j.priority)
				.toList();

		final var prioSort = allPjobs.stream()
				.skip(1)
				.sorted((l, r) -> Integer.compare(r.priority, l.priority))
				.map(j -> j.name + "_t" + j.startTime + "_P" + j.priority)
				.toList();

		assertEquals(prioSort, dateSort);

		checkSupervisableEventOnEnd(count * 3, true);

		verify(jobKitWatchdog, times(count)).addJob(any(WatchableSpoolJob.class));
		verify(jobKitWatchdog, times(count)).startJob(any(WatchableSpoolJob.class), anyLong());
		verify(jobKitWatchdog, atMost(count)).endJob(any(WatchableSpoolJob.class));
	}

	private void checkSupervisableEventOnEnd(final int count, final boolean normalyDone) {
		verify(sEvent, atLeast(count)).onEnd(any(), oExceptionCaptor.capture());
		for (final Optional<Exception> oE : oExceptionCaptor.getAllValues()) {
			if (normalyDone) {
				assertFalse(oE.isPresent());
			} else {
				assertTrue(oE.isPresent());
				assertEquals(IllegalArgumentException.class, oE.get().getClass());
			}
		}
	}

	private void verifyTotalBeforeStart(final int count) {
		verify(event, times(count)).beforeStart(any(String.class),
				any(long.class), any(SpoolExecutor.class));
	}

	private void verifyTotalAfterRunCorrectly(final int count) {
		verify(event, times(count)).afterRunCorrectly(any(String.class),
				any(long.class), any(long.class), any(SpoolExecutor.class));
	}

	private void verifyTotalAfterFailedRun(final int count) {
		verify(event, times(count)).afterFailedRun(any(String.class), any(long.class), any(long.class),
				any(SpoolExecutor.class), any(Exception.class));
	}

	@Test
	void testAddToQueue_beforeStartError() throws InterruptedException {
		final var error = new RuntimeException(Faker.instance().darkSoul().covenants());
		Mockito.doThrow(error).when(event)
				.beforeStart(any(String.class), any(long.class), eq(spoolExecutor));

		final var sm = new CountDownLatch(2);
		assertTrue(spoolExecutor.addToQueue(() -> {
			sm.countDown();
		}, name, 0, e -> {
			sm.countDown();
		}));
		assertTrue(sm.await(10, SECONDS));

		assertEquals(0, spoolExecutor.getQueueSize());
		while (spoolExecutor.isRunning()) {
			Thread.onSpinWait();
		}

		verify(sEvent, atLeast(3)).onEnd(any(), oExceptionCaptor.capture());

		final var results = oExceptionCaptor.getAllValues();

		assertEquals(error, results.get(0).get());
		assertFalse(results.get(1).isPresent());
		assertFalse(results.get(2).isPresent());

		checkWatchdog(1);
	}

	@Test
	void testAddToQueue_afterStartError() throws InterruptedException {
		final var error = new RuntimeException(Faker.instance().darkSoul().covenants());
		Mockito.doThrow(error).when(event)
				.afterRunCorrectly(any(String.class), any(long.class), any(long.class), eq(spoolExecutor));

		final var sm = new CountDownLatch(2);
		assertTrue(spoolExecutor.addToQueue(() -> {
			sm.countDown();
		}, name, 0, e -> {
			sm.countDown();
		}));
		assertTrue(sm.await(10, SECONDS));

		verify(sEvent, atLeast(3)).onEnd(any(), oExceptionCaptor.capture());

		final var results = oExceptionCaptor.getAllValues();

		assertFalse(results.get(0).isPresent());
		assertFalse(results.get(1).isPresent());
		assertEquals(error, results.get(2).get());
		checkWatchdog(atMost(1));
	}

	@Test
	void testWaitToEndQueue_1item() throws InterruptedException, ExecutionException, TimeoutException {
		spoolExecutor.waitToEndQueue(Runnable::run).get();

		final var smChkVerifyEvent = new CountDownLatch(1);
		final var smCmd = new CountDownLatch(1);
		final var smAfter = new CountDownLatch(1);

		spoolExecutor.addToQueue(() -> {
			try {
				smChkVerifyEvent.await(10, SECONDS);
			} catch (final InterruptedException e1) {
				throw new IllegalStateException(e1);
			}
			smCmd.countDown();
		}, name, 0, e -> {
			smAfter.countDown();
		});

		final var future = spoolExecutor.waitToEndQueue(newVirtualThreadPerTaskExecutor());

		assertFalse(future.isDone());
		assertFalse(future.isCancelled());

		smChkVerifyEvent.countDown();

		smCmd.await(10, SECONDS);
		smAfter.await(10, SECONDS);
		future.get(10, SECONDS);

		checkSupervisableEventOnEnd(3, true);
		checkWatchdog(1);
	}

	@Test
	void testWaitToEndQueue_10items() throws InterruptedException, ExecutionException, TimeoutException {
		final var size = 10;

		final var countDownLatchList = new ArrayList<CountDownLatch>();
		final var smCmd = new CountDownLatch(size);
		final var smAfter = new CountDownLatch(size);

		for (var pos = 0; pos < size; pos++) {
			final var latch = new CountDownLatch(1);
			countDownLatchList.add(latch);
			spoolExecutor.addToQueue(() -> {
				try {
					latch.await(10, SECONDS);
				} catch (final InterruptedException e1) {
					throw new IllegalStateException(e1);
				}
				smCmd.countDown();
			}, name, 0, e -> {
				smAfter.countDown();
			});
		}

		final var future = spoolExecutor.waitToEndQueue(newVirtualThreadPerTaskExecutor());

		assertFalse(future.isDone());
		assertFalse(future.isCancelled());

		for (var pos = 0; pos < size; pos++) {
			countDownLatchList.get(pos).countDown();
			Thread.sleep(1);// NOSONAR S2925
		}

		smCmd.await(10, SECONDS);
		smAfter.await(10, SECONDS);
		future.get(10, SECONDS);

		checkSupervisableEventOnEnd(3 * size, true);
		checkWatchdog(size);
	}

	@Test
	void testWaitToEndQueue_noInterblocking() {
		spoolExecutor.addToQueue(() -> {
			spoolExecutor.waitToEndQueue(Runnable::run).get(10, SECONDS);
		}, name, 0, e -> {
		});
		checkWatchdog(1);
	}

	private void checkWatchdog(final VerificationMode mode) {
		final var addCaptor = ArgumentCaptor.forClass(WatchableSpoolJob.class);
		verify(jobKitWatchdog, mode).addJob(addCaptor.capture());

		final var startCaptor = ArgumentCaptor.forClass(WatchableSpoolJob.class);
		final var startTimeCaptor = ArgumentCaptor.forClass(Long.class);
		verify(jobKitWatchdog, mode).startJob(startCaptor.capture(), startTimeCaptor.capture());

		final var endCaptor = ArgumentCaptor.forClass(WatchableSpoolJob.class);
		verify(jobKitWatchdog, mode).endJob(endCaptor.capture());

		final var addCaptorValues = addCaptor.getAllValues();
		final var startCaptorValues = startCaptor.getAllValues();
		final var endCaptorValues = endCaptor.getAllValues();

		final var max = Math.min(addCaptorValues.size(), Math.min(startCaptorValues.size(), endCaptorValues.size()));
		for (var pos = 0; pos < max; pos++) {
			assertEquals(addCaptorValues.get(pos), startCaptorValues.get(pos));
			assertEquals(endCaptorValues.get(pos), addCaptorValues.get(pos));
			final var startTime = startTimeCaptor.getAllValues().get(pos);
			assertTrue(startTime > 0l);
		}

	}

	private void checkWatchdog(final int total) {
		checkWatchdog(atLeast(total - 1));
	}

}
