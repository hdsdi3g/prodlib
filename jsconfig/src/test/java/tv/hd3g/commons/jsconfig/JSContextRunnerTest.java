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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;
import tv.hd3g.commons.jsconfig.mod.component.ContextBuilderProvider;

class JSContextRunnerTest {
	static final Faker faker = Faker.instance();

	File demoJs;
	File demo2Js;
	JSConfigConfig config;
	JSContextRunner r;
	File createdFile;
	File createdDir;
	String memberName;
	Object[] arguments;

	@Mock
	JSUpdateWatcher watcher;
	@Mock
	ContextBuilderProvider contextBuilderProvider;
	@Mock
	JSContextLoader jsContextLoader;
	@Mock
	Value binding;
	@Mock
	Value member;
	@Mock
	Value resultValue;
	@Mock
	Set<String> memberKeys;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		config = new JSConfigConfig();
		demoJs = new File("src/test/resources/demo.js").getAbsoluteFile();
		demo2Js = new File("src/test/resources/demo2.js").getAbsoluteFile();
		createdFile = new File("target/tempjsconfig/temp.js");
		createdDir = new File("target/tempjsconfig/" + faker.numerify("temp###"));

		FileUtils.deleteDirectory(createdDir.getParentFile());
		FileUtils.forceMkdir(createdDir);

		memberName = faker.numerify("memberName###");
		arguments = new String[] { faker.numerify("arg###"), faker.numerify("arg###") };

		when(contextBuilderProvider.newContextLoader(any())).thenReturn(jsContextLoader);
		when(jsContextLoader.getBinding()).thenReturn(binding);
		when(binding.getMember(memberName)).thenReturn(member);
		when(binding.getMemberKeys()).thenReturn(memberKeys);
		when(member.execute(arguments)).thenReturn(resultValue);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				contextBuilderProvider,
				jsContextLoader,
				binding,
				member,
				resultValue,
				memberKeys,
				watcher);
	}

	@Test
	void testSimple() throws InterruptedException {
		config.setSrc(List.of(demoJs));
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		checkWatcherStarts();

		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(demoJs));
	}

	@Test
	void testExecuteJSFunction() throws InterruptedException {
		testSimple();

		final var result = r.executeJSFunction(memberName, arguments);
		assertTrue(result.isPresent());
		assertEquals(resultValue, result.get());

		verify(jsContextLoader, times(1)).getBinding();
		verify(binding, times(1)).getMember(memberName);
		verify(member, times(1)).execute(arguments);
	}

	@Test
	void testExecuteJSFunction_noMember() throws InterruptedException {
		testSimple();
		when(binding.getMember(memberName)).thenReturn(null);
		final var result = r.executeJSFunction(memberName, arguments);
		assertFalse(result.isPresent());

		verify(jsContextLoader, times(1)).getBinding();
		verify(binding, times(1)).getMember(memberName);
	}

	@Test
	void testUpdateFile() throws IOException, InterruptedException {
		copyFile(demoJs, createdFile);
		config.setSrc(List.of(createdFile));
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		checkWatcherStarts();

		copyFile(demo2Js, createdFile);
		r.updateContext();

		assertEquals(memberKeys, r.getMemberKeys());
		verify(binding, times(1)).getMemberKeys();

		final var result = r.executeJSFunction(memberName, arguments);
		assertTrue(result.isPresent());
		assertEquals(resultValue, result.get());

		verify(contextBuilderProvider, times(2))
				.newContextLoader(Set.of(createdFile.getAbsoluteFile()));
		verify(jsContextLoader, times(2)).getBinding();
		verify(jsContextLoader, times(1)).close();
		verify(binding, times(1)).getMember(memberName);
		verify(member, times(1)).execute(arguments);
	}

	@Test
	void testCRUDFile() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		checkWatcherStarts();
		createdFile = new File(createdDir, faker.numerify("temp###.js"));

		copyFile(demoJs, createdFile);
		r.updateContext();

		copyFile(demo2Js, createdFile);
		write(new File(createdDir, faker.numerify("fake###.nojs")), "fake=", UTF_8);
		write(new File(createdDir, faker.numerify(".fake###.js")), "fake=", UTF_8);
		new File(createdDir, faker.numerify("subdir")).mkdirs();
		r.updateContext();

		FileUtils.delete(createdFile);
		r.updateContext();

		verify(contextBuilderProvider, times(2)).newContextLoader(Set.of());
		verify(contextBuilderProvider, times(2)).newContextLoader(Set.of(createdFile.getAbsoluteFile()));
		verify(jsContextLoader, times(3)).close();
	}

	@Test
	void testSortLoadJS() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		checkWatcherStarts();

		final var jsA = new File(createdDir, faker.numerify("a###.js")).getAbsoluteFile();
		final var jsB = new File(createdDir, faker.numerify("b###.js")).getAbsoluteFile();
		final var jsC = new File(createdDir, faker.numerify("c###.js")).getAbsoluteFile();
		final var jsZ = new File(createdDir, faker.numerify("d###.js")).getAbsoluteFile();

		write(jsB, "A", UTF_8);
		r.updateContext();
		write(jsZ, "A", UTF_8);
		r.updateContext();
		write(jsC, "A", UTF_8);
		r.updateContext();
		write(jsA, "A", UTF_8);
		r.updateContext();

		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of());
		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(jsB));
		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(jsB, jsZ));
		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(jsB, jsC, jsZ));
		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(jsA, jsB, jsC, jsZ));
		verify(jsContextLoader, times(4)).close();
	}

	@Test
	void testCantFoundFile() throws InterruptedException {
		config.setSrc(List.of(new File("src/test/resources/something")));
		assertThrows(UncheckedIOException.class, () -> new JSContextRunner(config, contextBuilderProvider, watcher));
	}

	@Test
	void testNoFile() throws InterruptedException {
		config.setSrc(List.of());
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		assertFalse(r.executeJSFunction("testJS", "").isPresent());
		assertTrue(r.getMemberKeys().isEmpty());
	}

	@Test
	void testDisableWatch() throws InterruptedException {
		config.setSrc(List.of(demoJs));
		config.setDisableWatchfolder(true);
		r = new JSContextRunner(config, contextBuilderProvider, watcher);
		verify(contextBuilderProvider, times(1)).newContextLoader(Set.of(demoJs));
	}

	@Test
	void testScanDir() throws InterruptedException {
		config = Mockito.mock(JSConfigConfig.class);
		when(config.getDirSrc()).thenReturn(Set.of(new File("src/test/resources/something")));
		when(config.getFileSrc()).thenReturn(Set.of());
		when(config.getFileDirSrc()).thenReturn(List.of(new File("src/test/resources/something")));

		assertThrows(UncheckedIOException.class, () -> new JSContextRunner(config, contextBuilderProvider, watcher));
		verify(config, Mockito.atLeastOnce()).getDirSrc();
		verify(config, Mockito.atLeastOnce()).getFileSrc();
		verify(config, Mockito.atLeastOnce()).getFileDirSrc();
		verifyNoMoreInteractions(config);
	}

	private void checkWatcherStarts() throws InterruptedException {
		verify(watcher, times(1)).setOnUpdate(any());
		verify(watcher, times(1)).start();
	}

}
