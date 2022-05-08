package tv.hd3g.jobkit.mod.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto.Item;
import tv.hd3g.testtools.HashCodeEqualsTest;

class BackgroundServiceIdDtoTest extends HashCodeEqualsTest {

	static Random random = new Random();

	@Mock
	Set<Item> servicesIds;
	BackgroundServiceIdDto backgroundServiceIdDto;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		backgroundServiceIdDto = new BackgroundServiceIdDto(servicesIds);
	}

	@Test
	void testGetServicesIds() {
		assertEquals(servicesIds, backgroundServiceIdDto.getServicesIds());
	}

	@Override
	protected Object[] makeSameInstances() {
		return new Object[] { backgroundServiceIdDto, new BackgroundServiceIdDto(servicesIds) };
	}

	@Nested
	class ItemTest {
		UUID uuid;
		String serviceName;
		String servicePoolName;

		Item item;

		@BeforeEach
		void init() {
			uuid = UUID.randomUUID();
			serviceName = String.valueOf(random.nextLong());
			servicePoolName = String.valueOf(random.nextLong());
			item = new Item(uuid, serviceName, servicePoolName);
		}

		@Test
		void testGetUuid() {
			assertEquals(uuid, item.getUuid());
		}

		@Test
		void testGetServiceName() {
			assertEquals(serviceName, item.getServiceName());
		}

		@Test
		void testGetServicePoolName() {
			assertEquals(servicePoolName, item.getServicePoolName());
		}
	}

}
