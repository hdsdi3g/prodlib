/*
 * This file is part of env-version.
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
package tv.hd3g.commons.version.mod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tv.hd3g.commons.version.EnvironmentVersion;

@SpringBootTest
class EnvVersionConfigurerTest {

	@Autowired
	EnvironmentVersion ev;

	@Test
	void testGetEnvVersion() {
		assertNotNull(ev.appVersion(), "You will need a valid maven setup here to test");
		assertNotNull(ev.prodlibVersion());
		assertNotNull(ev.frameworkVersion());
		assertEquals(ev.prodlibVersion(), ev.appVersion());
	}

}
