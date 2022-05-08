package tv.hd3g.jobkit.mod;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.status.BackgroundServiceStatus;

class BackgroundServiceIdTest {
	static Random random = new Random();

	@Mock
	BackgroundService backgroundService;
	@Mock
	BackgroundServiceStatus backgroundServiceStatus;

	String name;
	String spoolName;
	BackgroundServiceId backgroundServiceId;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = String.valueOf(random.nextLong());
		spoolName = String.valueOf(random.nextLong());

		when(backgroundService.getLastStatus()).thenReturn(backgroundServiceStatus);
		when(backgroundServiceStatus.getName()).thenReturn(name);
		when(backgroundServiceStatus.getSpoolName()).thenReturn(spoolName);

		backgroundServiceId = new BackgroundServiceId();
	}

	@Test
	void testRegister() {
		final var uuid = backgroundServiceId.register(backgroundService);
		assertNotNull(uuid);
	}

	@Test
	void testUnRegister() {
		final var removed = backgroundServiceId.unRegister(randomUUID());
		assertNull(removed);

		final var uuid = backgroundServiceId.register(backgroundService);
		final var removed2 = backgroundServiceId.unRegister(uuid);
		assertEquals(backgroundService, removed2);
		final var nope = backgroundServiceId.getByUUID(uuid);
		assertNull(nope);
	}

	@Test
	void testGetAllRegisted() {
		final var result = backgroundServiceId.getAllRegistedAsDto();
		assertNotNull(result);
		assertEquals(0, result.getServicesIds().size());

		final var uuid = backgroundServiceId.register(backgroundService);
		final var result2 = backgroundServiceId.getAllRegistedAsDto();
		assertNotNull(result2);
		assertEquals(1, result2.getServicesIds().size());
		final var dto = result2.getServicesIds().stream().findFirst().get();
		assertEquals(uuid, dto.getUuid());
		assertEquals(name, dto.getServiceName());
		assertEquals(spoolName, dto.getServicePoolName());
	}

	@Test
	void testGetByUUID() {
		final var nope = backgroundServiceId.getByUUID(randomUUID());
		assertNull(nope);
		final var uuid = backgroundServiceId.register(backgroundService);
		assertEquals(backgroundService, backgroundServiceId.getByUUID(uuid));
	}

	@Test
	void testForEach() {
		backgroundServiceId.register(backgroundService);

		final var list = new ArrayList<>();
		backgroundServiceId.forEach(bSi -> {
			list.add(bSi);
		});
		assertEquals(1, list.size());
		assertEquals(backgroundService, list.get(0));
	}

}
