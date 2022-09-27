package tv.hd3g.jobkit.mod;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.mod.JobKitAsyncConfigurer.POOL_NAME;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.RunnableWithException;

class JobKitAsyncConfigurerTest {

	static Random random = new Random();

	@Mock
	JobKitEngine jobKitEngine;

	JobKitAsyncConfigurer jobKitAsyncConfigurer;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		jobKitAsyncConfigurer = new JobKitAsyncConfigurer();
		jobKitAsyncConfigurer.jobKitEngine = jobKitEngine;
	}

	@Test
	void testGetAsyncExecutor() {
		final var executor = jobKitAsyncConfigurer.getAsyncExecutor();
		assertNotNull(executor);

		when(jobKitEngine.runOneShot(anyString(), anyString(), anyInt(), any(RunnableWithException.class), any()))
		        .thenReturn(true);

		final var task = mock(Runnable.class);
		executor.execute(task);

		verify(jobKitEngine, times(1)).runOneShot(
		        startsWith("SpringBoot Async"), eq(POOL_NAME), eq(0),
		        any(RunnableWithException.class), any());
	}

	@Test
	void testGetAsyncUncaughtExceptionHandler() throws NoSuchMethodException, SecurityException {
		final var handler = jobKitAsyncConfigurer.getAsyncUncaughtExceptionHandler();
		assertNotNull(handler);
		handler.handleUncaughtException(new Throwable("message"), getClass().getMethods()[0]);
	}

}
