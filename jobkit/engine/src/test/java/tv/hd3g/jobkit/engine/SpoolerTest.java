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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;

class SpoolerTest {

	static Faker faker = net.datafaker.Faker.instance();

	@Mock
	ExecutionEvent event;

	Spooler spooler;
	String a;
	String b;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		a = faker.numerify("spoolA###");
		b = faker.numerify("spoolB###");

		spooler = new Spooler(event, new SupervisableEvents() {});
	}

	@Test
	void testGetExecutor() {
		final var aExec = spooler.getExecutor(a);
		assertNotNull(aExec);
		final var bExec = spooler.getExecutor(b);
		assertNotNull(bExec);
		assertNotEquals(aExec, bExec);
		final var aBisExec = spooler.getExecutor(a);
		assertNotNull(bExec);
		assertEquals(aBisExec, aExec);
	}

	@Test
	void testGetAllQueuesSize() {
		assertEquals(0, spooler.getAllQueuesSize());
		spooler.getExecutor(a);
		assertEquals(0, spooler.getAllQueuesSize());
	}

	@Test
	void testGetRunningQueuesCount() {
		assertEquals(0, spooler.getRunningQueuesCount());
		spooler.getExecutor(a);
		assertEquals(0, spooler.getRunningQueuesCount());
	}

	@Test
	void testShutdown_noKeepRun() {
		assertTrue(spooler.getExecutor(a)
				.addToQueue(nothing, "empty1", 0, e -> {
				}));

		spooler.shutdown(Set.of());

		assertFalse(spooler.getExecutor(a)
				.addToQueue(nothing, "empty2", 0, e -> {
				}));

		assertNull(spooler.getExecutor(b));
		verify(event, Mockito.times(1)).shutdownSpooler(any(Supervisable.class));

		spooler.shutdown(Set.of());
	}

	@Test
	void testShutdown_keepRun() {
		final var total = 100;
		final var countA = new AtomicInteger(0);
		final var countB = new AtomicInteger(0);

		for (var pos = 0; pos < total; pos++) {
			assertTrue(spooler.getExecutor(a).addToQueue(() -> {
				countA.getAndIncrement();
				Thread.sleep(1);// NOSONAR
			}, "taskA #" + pos, 0, e -> {
				countA.getAndIncrement();
			}));
			assertTrue(spooler.getExecutor(b).addToQueue(() -> {
				countB.getAndIncrement();
				Thread.sleep(1);// NOSONAR
			}, "taskB #" + pos, 0, e -> {
				countB.getAndIncrement();
			}));
		}
		spooler.shutdown(Set.of(a));

		assertEquals(total * 2, countA.get());
		assertTrue(total * 2 > countB.get());

		verify(event, Mockito.times(1)).shutdownSpooler(any(Supervisable.class));
	}

	@Test
	void testShutdown_empty() {
		spooler.shutdown(Set.of());

		assertNull(spooler.getExecutor(b));
		verify(event, Mockito.times(1)).shutdownSpooler(any(Supervisable.class));
	}
}
