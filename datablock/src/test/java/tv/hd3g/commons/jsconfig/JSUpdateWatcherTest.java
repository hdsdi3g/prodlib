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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

class JSUpdateWatcherTest {
	static Faker faker = net.datafaker.Faker.instance();

	JSUpdateWatcher u;
	JSConfigConfig config;

	CountDownLatch latch;
	File createdDir;
	Runnable onUpdate;

	@BeforeEach
	void init() throws Exception {
		config = new JSConfigConfig();
		createdDir = new File("target/tempjsconfig/" + faker.numerify("temp###"));

		deleteDirectory(createdDir.getParentFile());
		forceMkdir(createdDir);

		latch = new CountDownLatch(1);
		onUpdate = () -> latch.countDown();
	}

	@Test
	void testRun_AddedFile() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		u = new JSUpdateWatcher(config);
		u.setOnUpdate(onUpdate);
		u.start();
		assertTrue(u.isInitiated());

		final var jsA = new File(createdDir, faker.numerify("a###.js")).getAbsoluteFile();
		write(jsA, "A", UTF_8);
		assertTrue(latch.await(5, SECONDS));
	}

	@Test
	void testRun_EditedAddedFile() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));
		final var jsA = new File(createdDir, faker.numerify("a###.js")).getAbsoluteFile();
		write(jsA, "A", UTF_8);

		u = new JSUpdateWatcher(config);
		u.setOnUpdate(onUpdate);
		u.start();
		assertTrue(u.isInitiated());
		write(jsA, "B", UTF_8);
		assertTrue(latch.await(5, SECONDS));
	}

	@Test
	void testRun_EditedFile() throws IOException, InterruptedException {
		final var jsA = new File(createdDir, faker.numerify("a###.js")).getAbsoluteFile();
		write(jsA, "A", UTF_8);
		config.setSrc(List.of(jsA));

		u = new JSUpdateWatcher(config);
		u.setOnUpdate(onUpdate);
		u.start();
		assertTrue(u.isInitiated());
		write(jsA, "B", UTF_8);
		assertTrue(latch.await(5, SECONDS));
	}

	@Test
	void testRun_Ignored() throws IOException, InterruptedException {
		config.setSrc(List.of(createdDir));

		u = new JSUpdateWatcher(config);
		u.setOnUpdate(onUpdate);
		u.start();
		assertTrue(u.isInitiated());

		forceMkdir(new File(createdDir, faker.numerify("a###")));
		var jsA = new File(createdDir, faker.numerify(".hidded###.js")).getAbsoluteFile();
		write(jsA, "A", UTF_8);
		jsA = new File(createdDir, faker.numerify("notjs###.foobar")).getAbsoluteFile();
		write(jsA, "A", UTF_8);

		assertFalse(latch.await(1, SECONDS));
	}

}
