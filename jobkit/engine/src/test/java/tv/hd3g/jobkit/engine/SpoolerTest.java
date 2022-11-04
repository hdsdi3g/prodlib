package tv.hd3g.jobkit.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static tv.hd3g.jobkit.engine.RunnableWithException.nothing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class SpoolerTest {

	@Mock
	ExecutionEvent event;

	Spooler spooler;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		spooler = new Spooler(event, new SupervisableEvents() {});
	}

	@Test
	void testGetExecutor() {
		final var aExec = spooler.getExecutor("A");
		assertNotNull(aExec);
		final var bExec = spooler.getExecutor("B");
		assertNotNull(bExec);
		assertNotEquals(aExec, bExec);
		final var aBisExec = spooler.getExecutor("A");
		assertNotNull(bExec);
		assertEquals(aBisExec, aExec);
	}

	@Test
	void testGetAllQueuesSize() {
		assertEquals(0, spooler.getAllQueuesSize());
		spooler.getExecutor("A");
		assertEquals(0, spooler.getAllQueuesSize());
	}

	@Test
	void testGetRunningQueuesCount() {
		assertEquals(0, spooler.getRunningQueuesCount());
		spooler.getExecutor("A");
		assertEquals(0, spooler.getRunningQueuesCount());
	}

	@Test
	void testShutdown() {
		final var before = spooler.getExecutor("A").addToQueue(nothing, "empty1", 0, e -> {
		});
		assertTrue(before);

		spooler.shutdown();

		final var after = spooler.getExecutor("A").addToQueue(nothing, "empty2", 0, e -> {
		});
		assertFalse(after);

		assertNull(spooler.getExecutor("B"));
		verify(event, Mockito.times(1)).shutdownSpooler(any(Supervisable.class));

		spooler.shutdown();
	}

	@Test
	void testShutdown_empty() {
		spooler.shutdown();

		assertNull(spooler.getExecutor("B"));
		verify(event, Mockito.times(1)).shutdownSpooler(any(Supervisable.class));
	}
}
