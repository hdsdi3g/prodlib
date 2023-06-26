/*
 * This file is part of jobkit-engine.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.engine.watchdog;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Optional;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@EqualsAndHashCode
abstract class LimitedSpoolsBasePolicy implements JobWatchdogPolicy {

	private Set<String> onlySpools;
	private Set<String> notSpools;

	public Set<String> getOnlySpools() {
		return Optional.ofNullable(onlySpools).stream()
				.flatMap(Set::stream)
				.collect(toUnmodifiableSet());
	}

	public Set<String> getNotSpools() {
		return Optional.ofNullable(notSpools).stream()
				.flatMap(Set::stream)
				.collect(toUnmodifiableSet());
	}

	protected boolean allowSpool(final String spoolName) {
		final var allow = getOnlySpools();
		if (allow.isEmpty() == false) {
			return allow.contains(spoolName);
		}

		final var deny = getNotSpools();
		if (deny.isEmpty() == false) {
			return deny.contains(spoolName) == false;
		}
		return true;
	}

}
