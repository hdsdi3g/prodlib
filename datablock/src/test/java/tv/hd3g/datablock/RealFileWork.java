/*
 * This file is part of datablock.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package tv.hd3g.datablock;

import static org.apache.commons.io.FileUtils.delete;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import net.datafaker.Faker;

public class RealFileWork {

	static Faker faker = net.datafaker.Faker.instance();
	static byte[] data;
	static File file;

	@BeforeAll
	static void prepare() throws Exception {
		final var dir = new File("target/test-temp");
		forceMkdir(dir);

		data = faker.random().nextRandomBytes(faker.random().nextInt(1000, 10000));
		file = File.createTempFile(
				DatablockInputStreamChunkTest.class.getSimpleName(),
				".bin",
				dir);
	}

	@AfterAll
	static void close() throws IOException {
		if (file != null) {
			delete(file);
		}
	}

	FileChannel channel;

	@AfterEach
	void end() throws Exception {
		if (channel == null) {
			return;
		}
		try {
			channel.close();
		} catch (final IOException e) {
		}
	}

}
