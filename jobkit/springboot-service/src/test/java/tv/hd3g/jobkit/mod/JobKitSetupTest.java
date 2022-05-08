package tv.hd3g.jobkit.mod;

import static java.lang.Thread.MIN_PRIORITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.ResourceBundleMessageSource;

import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;

class JobKitSetupTest {

	@Mock
	ScheduledExecutorService scheduledExecutor;
	@Mock
	ExecutionEvent executionEvent;
	@Mock
	BackgroundServiceEvent backgroundServiceEvent;
	@Mock
	ResourceBundleMessageSource resourceBundleMessageSource;

	JobKitSetup jobKitSetup;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		jobKitSetup = new JobKitSetup();
	}

	@Test
	void testGetScheduledExecutor() throws InterruptedException {
		final var sch = jobKitSetup.getScheduledExecutor();
		assertNotNull(sch);

		final var currentThread = new AtomicReference<Thread>();
		final var latch = new CountDownLatch(1);
		sch.execute(() -> {
			currentThread.set(Thread.currentThread());
			latch.countDown();
		});
		latch.await(1, TimeUnit.SECONDS);

		final var thread = currentThread.get();
		assertNotNull(thread);
		assertEquals("SchTaskStarter", thread.getName());
		assertEquals(MIN_PRIORITY + 1, thread.getPriority());
		assertEquals(false, thread.isDaemon());
		assertNotNull(thread.getUncaughtExceptionHandler());
	}

	@Test
	void testGetJobKitEngine() {
		assertNotNull(jobKitSetup.getJobKitEngine(scheduledExecutor, executionEvent, backgroundServiceEvent));
	}

	@Test
	void testGetBackgroundServiceId() {
		assertNotNull(jobKitSetup.getBackgroundServiceId());
	}

}
