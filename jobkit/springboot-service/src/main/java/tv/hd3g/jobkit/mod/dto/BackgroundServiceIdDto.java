package tv.hd3g.jobkit.mod.dto;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

public class BackgroundServiceIdDto extends RepresentationModel<BackgroundServiceIdDto> {

	private final Set<Item> servicesIds;

	public BackgroundServiceIdDto(final Set<Item> servicesIds) {
		this.servicesIds = servicesIds;
	}

	public static class Item {

		private final UUID uuid;
		private final String serviceName;
		private final String servicePoolName;

		public Item(final UUID uuid, final String serviceName, final String servicePoolName) {
			this.uuid = uuid;
			this.serviceName = serviceName;
			this.servicePoolName = servicePoolName;
		}

		public UUID getUuid() {
			return uuid;
		}

		public String getServiceName() {
			return serviceName;
		}

		public String getServicePoolName() {
			return servicePoolName;
		}
	}

	public Set<Item> getServicesIds() {
		return servicesIds;
	}

	@Override
	public int hashCode() {
		final var prime = 31;
		var result = super.hashCode();
		result = prime * result + Objects.hash(servicesIds);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (BackgroundServiceIdDto) obj;
		return Objects.equals(servicesIds, other.servicesIds);
	}
}
