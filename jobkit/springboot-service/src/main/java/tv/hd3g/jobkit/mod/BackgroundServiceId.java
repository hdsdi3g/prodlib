package tv.hd3g.jobkit.mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto;
import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto.Item;

public class BackgroundServiceId {

	private final ConcurrentHashMap<UUID, BackgroundService> serviceMap;

	public BackgroundServiceId() {
		serviceMap = new ConcurrentHashMap<>();
	}

	public UUID register(final BackgroundService service) {
		final var key = UUID.randomUUID();
		serviceMap.put(key, service);
		return key;
	}

	public BackgroundService unRegister(final UUID uuid) {
		return serviceMap.remove(uuid);
	}

	public BackgroundServiceIdDto getAllRegistedAsDto() {
		return new BackgroundServiceIdDto(serviceMap.entrySet().stream().map(entry -> {
			final var lastStatus = entry.getValue().getLastStatus();
			return new Item(entry.getKey(), lastStatus.getName(), lastStatus.getSpoolName());
		}).collect(Collectors.toUnmodifiableSet()));
	}

	public BackgroundService getByUUID(final UUID uuid) {
		return serviceMap.get(uuid);
	}

	public void forEach(final Consumer<BackgroundService> action) {
		serviceMap.values().forEach(action);
	}

}
