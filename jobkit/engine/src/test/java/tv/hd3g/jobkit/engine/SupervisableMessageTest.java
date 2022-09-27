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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class SupervisableMessageTest {
	static Faker faker = Faker.instance();

	SupervisableMessage supervisableMessage;

	String code;
	String defaultResult;
	Object[] vars;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		code = faker.dog().name();
		defaultResult = faker.cat().name();
		vars = new Object[] { faker.app().name(), new Object() };
		supervisableMessage = new SupervisableMessage(code, defaultResult, vars);
	}

	@Test
	void testGetVarsArray() {
		assertArrayEquals(new String[] { (String) vars[0], String.valueOf(vars[1]) },
				supervisableMessage.getVarsArray());

	}

	@Test
	void testGetVarsArray_Empty() {
		supervisableMessage = new SupervisableMessage(code, defaultResult, new String[] {});
		assertArrayEquals(new String[] {}, supervisableMessage.getVarsArray());
	}

}
