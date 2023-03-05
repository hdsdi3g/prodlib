/*
 * This file is part of interfaces.
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
package tv.hd3g.commons.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class EnvironmentVersionTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Test
	void testMake() {
		final var appVersion = faker.numerify("appVersion###");
		final var prodlibVersion = faker.numerify("prodlibVersion###");
		final var frameworkVersion = faker.numerify("frameworkVersion###");

		final var v = EnvironmentVersion.makeEnvironmentVersion(appVersion, prodlibVersion, frameworkVersion);

		assertNotNull(v);
		assertEquals(appVersion, v.appVersion());
		assertEquals(prodlibVersion, v.prodlibVersion());
		assertEquals(frameworkVersion, v.frameworkVersion());
		assertNotNull(v.jvmVersion());
		assertNotNull(v.jvmNameVendor());
		assertTrue(v.pid() > 0l);
		assertTrue(v.startupTime().getTime() > 0);
	}

}
