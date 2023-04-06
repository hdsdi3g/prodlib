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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

class JSContextLoaderTest {
	static final Faker faker = Faker.instance();

	JSContextLoader j;
	JSConfigConfig config;

	@Mock
	Builder contextBuilder;
	@Mock
	Context context;
	@Mock
	Value binding;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		config = new JSConfigConfig();
		when(contextBuilder.build()).thenReturn(context);
		when(context.getBindings("js")).thenReturn(binding);

		j = new JSContextLoader(Set.of(new File("src/test/resources/demo.js")), config, contextBuilder);

		verify(contextBuilder, times(1)).build();
		verify(context, times(1)).eval("js", """
				function testJS(inputValue) {
				    return inputValue + "-test";
				}
				""");
		verify(context, times(1)).getBindings("js");
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(contextBuilder, context, binding);
	}

	@Test
	void testClose() {
		j.close();
		verify(context, times(1)).close();
		assertEquals(binding, j.getBinding());
	}

	@Test
	void testGetBinding() {
		assertEquals(binding, j.getBinding());
	}

}
