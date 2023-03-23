/*
 * This file is part of jsconfig.
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
package tv.hd3g.commons.jsconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

class JSConfigContextTest {
	static final Faker faker = Faker.instance();

	JSConfigContext j;
	JSConfigConfig config;

	@BeforeEach
	void init() {
		config = new JSConfigConfig();
		j = new JSConfigContext(Set.of(new File("src/test/resources/demo.js")), config);
	}

	@AfterEach
	void ends() {
		j.close();
	}

	@Test
	void testClose() {
		j.close();
		final var b = j.getBinding();
		assertThrows(IllegalStateException.class, () -> b.getMember("testJS"));
	}

	@Test
	void testGetBinding() {
		final var arg = faker.numerify("argument###");
		final var result = j.getBinding().getMember("testJS").execute(arg);
		assertEquals(arg + "-test", result.asString());
	}

}
