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
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import net.datafaker.Faker;

public class RealFileWork {

	private static final File dir;

	static {
		dir = new File("target/test-temp");
		try {
			forceMkdir(dir);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't prepare dir", e);
		}
	}

	static Faker faker = net.datafaker.Faker.instance();
	static byte[] data;
	static File file;
	static File file1;

	@BeforeAll
	static void prepare() throws Exception {
		data = faker.random().nextRandomBytes(faker.random().nextInt(1000, 10000));
		file = File.createTempFile(
				DatablockInputStreamChunkTest.class.getSimpleName(),
				".bin",
				dir);
	}

	void prepareFile1() throws IOException {
		file1 = File.createTempFile(
				DatablockInputStreamChunkTest.class.getSimpleName(),
				".bin",
				dir);
	}

	@AfterAll
	static void close() throws IOException {
		if (file != null && file.exists()) {
			delete(file);
		}
		if (file1 != null && file1.exists()) {
			delete(file1);
		}
	}

	FileChannel channel;
	FileChannel channel1;

	@AfterEach
	void ends() {
		closeChannel(channel);
		closeChannel(channel1);
	}

	private static void closeChannel(final FileChannel channel) {
		if (channel == null) {
			return;
		}
		try {
			channel.close();
		} catch (final IOException e) {
			/**
			 * Silent close channel
			 */
		}
	}

}
