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
package tv.hd3g.commons.jsconfig.mod.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

@SpringBootTest
class ContextBuilderProviderTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	ContextBuilderProvider p;

	@MockBean
	JSConfigConfig jsConfigConfig;
	File demoJs;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		demoJs = new File("src/test/resources/demo.js").getAbsoluteFile();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(jsConfigConfig);
	}

	@Test
	void testNewBuilder() throws IOException {
		final var builder = p.newBuilder();
		assertNotNull(builder);

		final var context = builder.build();
		context.eval("js", FileUtils.readFileToString(demoJs, UTF_8));
		final var binding = context.getBindings("js");
		final var arg = faker.numerify("argument###");
		assertEquals(arg + "-test", binding.getMember("testJS").execute(arg).asString());

		checkConfigJSEngine();
		verify(jsConfigConfig, atLeastOnce()).getFileSrc();
		verify(jsConfigConfig, atLeastOnce()).getDirSrc();
		verify(jsConfigConfig, atLeastOnce()).getFileDirSrc();
	}

	@Test
	void testNewContextLoader() {
		final var jsContextLoader = p.newContextLoader(Set.of(demoJs));
		assertNotNull(jsContextLoader);

		final var arg = faker.numerify("argument###");
		assertEquals(arg + "-test", jsContextLoader.getBinding().getMember("testJS").execute(arg).asString());

		checkConfigJSEngine();
		verify(jsConfigConfig, atLeastOnce()).isFineLevelLogger();
		verify(jsConfigConfig, atLeastOnce()).getCurrentWorkingDirectory();
	}

	@Test
	void testNewContextLoader_empty() {
		final var jsContextLoader = p.newContextLoader(Set.of());
		assertNotNull(jsContextLoader);
		assertNull(jsContextLoader.getBinding().getMember("testJS"));

		checkConfigJSEngine();
		verify(jsConfigConfig, atLeastOnce()).isFineLevelLogger();
		verify(jsConfigConfig, atLeastOnce()).getCurrentWorkingDirectory();
	}

	private void checkConfigJSEngine() {
		verify(jsConfigConfig, times(1)).isAllowCreateProcess();
		verify(jsConfigConfig, times(1)).isAllowCreateThread();
		verify(jsConfigConfig, times(1)).isAllowExperimentalOptions();
		verify(jsConfigConfig, times(1)).isDisableHostClassLoading();
		verify(jsConfigConfig, times(1)).isAllowIO();
		verify(jsConfigConfig, times(1)).isAllowNativeAccess();
	}

}
