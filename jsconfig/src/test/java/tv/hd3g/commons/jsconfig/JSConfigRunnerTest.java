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
import static org.apache.commons.io.FileUtils.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

class JSConfigRunnerTest {
	static final Faker faker = Faker.instance();

	File demoJs;
	File demo2Js;
	JSConfigConfig config;
	JSConfigRunner r;
	File createdFile;
	File createdDir;

	@BeforeEach
	void init() throws IOException {
		config = new JSConfigConfig();
		demoJs = new File("src/test/resources/demo.js").getAbsoluteFile();
		demo2Js = new File("src/test/resources/demo2.js").getAbsoluteFile();
		createdFile = new File("target/tempjsconfig/temp.js");
		createdDir = new File("target/tempjsconfig/" + faker.numerify("temp###"));

		FileUtils.deleteDirectory(createdDir.getParentFile());
		FileUtils.forceMkdir(createdDir);
	}

	@Test
	void testSimple() {
		config.setSrc(List.of(demoJs));
		r = new JSConfigRunner(config);
		checkExecute_testJS();
	}

	@Test
	void testCantFoundFile() {
		config.setSrc(List.of(new File("src/test/resources/something")));
		assertThrows(UncheckedIOException.class, () -> new JSConfigRunner(config));
	}

	@Test
	void testNoFile() {
		config.setSrc(List.of());
		r = new JSConfigRunner(config);
		assertFalse(r.executeJSFunction("testJS", "").isPresent());
	}

	@Test
	void testUpdateFile() throws IOException, InterruptedException {
		FileUtils.copyFile(demoJs, createdFile);
		config.setSrc(List.of(createdFile));
		r = new JSConfigRunner(config);
		Thread.sleep(100);// NOSONAR S2925
		checkExecute_testJS();
		FileUtils.copyFile(demo2Js, createdFile);

		Thread.sleep(100);// NOSONAR S2925

		assertEquals(Set.of(createdFile.getAbsoluteFile()), r.getAllJSFiles());
		assertEquals(Set.of("testJS"), r.getMemberKeys());
		checkExecute_test2JS();
	}

	@Test
	void testCRUDFile() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		r = new JSConfigRunner(config);
		assertFalse(r.executeJSFunction("testJS", "").isPresent());

		createdFile = new File(createdDir, faker.numerify("temp###.js"));
		FileUtils.copyFile(demoJs, createdFile);

		Thread.sleep(10);// NOSONAR S2925
		checkExecute_testJS();

		Thread.sleep(10);// NOSONAR S2925
		FileUtils.copyFile(demo2Js, createdFile);

		FileUtils.write(new File(createdDir, faker.numerify("fake###.nojs")), "fake=", UTF_8);
		FileUtils.write(new File(createdDir, faker.numerify(".fake###.js")), "fake=", UTF_8);
		new File(createdDir, faker.numerify("subdir")).mkdirs();

		Thread.sleep(100);// NOSONAR S2925
		checkExecute_test2JS();

		FileUtils.delete(createdFile);
		Thread.sleep(10);// NOSONAR S2925
		assertFalse(r.executeJSFunction("testJS", "").isPresent());
	}

	@Test
	void testSortLoadJS() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		r = new JSConfigRunner(config);

		final var expectedValue = faker.numerify("value###");
		final var jsA = new File(createdDir, faker.numerify("a###.js"));
		final var jsB = new File(createdDir, faker.numerify("b###.js"));
		final var jsC = new File(createdDir, faker.numerify("c###.js"));
		final var jsZ = new File(createdDir, faker.numerify("d###.js"));

		write(jsB, "value=\"" + faker.numerify("bad###") + "\";", UTF_8);
		write(jsZ, """
				function testSortJS() {
				    return value;
				}
				""", UTF_8);
		write(jsC, "value=\"" + expectedValue + "\";", UTF_8);
		write(jsA, "value=\"" + faker.numerify("bad###") + "\";", UTF_8);

		Thread.sleep(100);// NOSONAR S2925
		assertEquals(Set.of(jsA.getAbsoluteFile(), jsB.getAbsoluteFile(), jsC.getAbsoluteFile(), jsZ.getAbsoluteFile()),
				r.getAllJSFiles());
		assertEquals(Set.of("testSortJS", "value"), r.getMemberKeys());

		final var result = r.executeJSFunction("testSortJS");
		assertTrue(result.isPresent());
		assertEquals(expectedValue, result.get().asString());
	}

	@Test
	void testGetAllJSFiles() {
		config.setSrc(List.of(demoJs));
		r = new JSConfigRunner(config);
		assertEquals(Set.of(demoJs), r.getAllJSFiles());
	}

	void checkExecute_testJS() {
		final var arg = faker.numerify("argument###");
		final var result = r.executeJSFunction("testJS", arg);
		assertTrue(result.isPresent());
		assertEquals(arg + "-test", result.get().asString());
	}

	void checkExecute_test2JS() {
		final var arg = faker.numerify("argument###");
		final var result = r.executeJSFunction("testJS", arg);
		assertTrue(result.isPresent());
		assertEquals(arg + "-test2", result.get().asString());
	}

}
