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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class LimitedSpoolsBasePolicyTest {

	private static Faker faker = net.datafaker.Faker.instance();

	class Policy extends LimitedSpoolsBasePolicy {

		@Override
		public Optional<Duration> isStatusOk(final String spoolName,
											 final WatchableSpoolJobState activeJob,
											 final Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning {
			throw new UnsupportedOperationException();
		}

		@Override
		public void isStatusOk(final String spoolName,
							   final WatchableSpoolJobState activeJob,
							   final Set<WatchableSpoolJobState> queuedJobs,
							   final Set<WatchableBackgroundService> relativeBackgroundServices) throws JobWatchdogPolicyWarning {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getDescription() {
			throw new UnsupportedOperationException();
		}

	}

	Policy p;
	Set<String> onlySpools;
	Set<String> notSpools;
	String value;

	@BeforeEach
	void init() throws Exception {
		p = new Policy();
		onlySpools = new HashSet<>();
		notSpools = new HashSet<>();
		value = faker.numerify("###");
	}

	@Test
	void testGetOnlySpools() {
		assertEquals(Set.of(), p.getOnlySpools());
		assertThrows(UnsupportedOperationException.class, () -> p.getOnlySpools().add("")); // NOSONAR S5778
		p.setOnlySpools(onlySpools);
		assertEquals(Set.of(), p.getOnlySpools());
		assertThrows(UnsupportedOperationException.class, () -> p.getOnlySpools().add("")); // NOSONAR S5778
		onlySpools.add(value);
		assertEquals(Set.of(value), p.getOnlySpools());
	}

	@Test
	void testGetNotSpools() {
		assertEquals(Set.of(), p.getNotSpools());
		assertThrows(UnsupportedOperationException.class, () -> p.getNotSpools().add("")); // NOSONAR S5778
		p.setNotSpools(notSpools);
		assertEquals(Set.of(), p.getNotSpools());
		assertThrows(UnsupportedOperationException.class, () -> p.getNotSpools().add("")); // NOSONAR S5778
		notSpools.add(value);
		assertEquals(Set.of(value), p.getNotSpools());
	}

	@Test
	void testAllowSpool() {
		p.setNotSpools(notSpools);
		p.setOnlySpools(onlySpools);

		assertTrue(p.allowSpool(value));
		notSpools.add("A");
		assertTrue(p.allowSpool(value));
		assertFalse(p.allowSpool("A"));

		notSpools.clear();
		onlySpools.add("A");
		assertFalse(p.allowSpool(value));
		assertTrue(p.allowSpool("A"));

		notSpools.add("A");
		assertTrue(p.allowSpool("A"));
		assertFalse(p.allowSpool(value));
	}
}
