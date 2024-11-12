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

import static java.nio.file.StandardOpenOption.READ;
import static java.util.Arrays.fill;
import static org.apache.commons.io.FileUtils.delete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class DatablockInputStreamChunkTest {

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
		writeByteArrayToFile(file, data);
	}

	@AfterAll
	static void close() throws IOException {
		if (file != null) {
			delete(file);
		}
	}

	FileChannel channel;
	int payloadPosition;
	int payloadSize;
	DatablockInputStreamChunk c;

	@BeforeEach
	void init() throws Exception {
		channel = FileChannel.open(file.toPath(), READ);
		payloadPosition = faker.random().nextInt(data.length / 12, data.length / 10);
		final var from = data.length / 20;
		final var to = data.length - payloadPosition - from - data.length / 20;
		payloadSize = faker.random().nextInt(from, to);

		c = new DatablockInputStreamChunk(channel, payloadPosition, payloadSize);
		assertEquals(payloadPosition, channel.position());
		assertEquals(payloadSize, c.available());
	}

	@AfterEach
	void end() throws Exception {
		channel.close();
	}

	@Test
	void testRead() throws IOException {
		for (var pos = 0; pos < payloadSize; pos++) {
			assertEquals(data[pos + payloadPosition] & 0xFF, c.read());
		}
		assertEquals(-1, c.read());
		checkEOF();
	}

	@Test
	void testReadByteArrayIntInt_full() throws IOException {
		final var result = new byte[payloadSize];
		assertEquals(payloadSize, c.read(result, 0, result.length));
		compareFullCopy(result);

		checkEOF();

		fill(result, (byte) 0);
		assertEquals(-1, c.read(result, 0, result.length));
		assertArrayEquals(new byte[result.length], result);

		checkEOF();
	}

	@Test
	void testReadByteArrayIntInt_partial() throws IOException {
		final var skip = faker.random().nextInt(0, 10);
		c.skip(skip);

		final var result = new byte[payloadSize - payloadSize / 10];
		final var startFromRead = result.length / 10;
		final var reeded = result.length - payloadSize / 10;
		assertEquals(reeded, c.read(result, startFromRead, reeded));
		assertEquals(payloadSize - reeded - skip, c.available());

		assertTrue(Arrays.equals(
				data, skip + payloadPosition, skip + payloadPosition + reeded,
				result, startFromRead, startFromRead + reeded));

		assertTrue(Arrays.equals(
				new byte[startFromRead], 0, startFromRead,
				result, 0, startFromRead));

		assertTrue(Arrays.equals(
				new byte[result.length - startFromRead - reeded], 0, result.length - startFromRead - reeded,
				result, startFromRead + reeded, result.length));
	}

	@Test
	void testTransferTo() throws IOException {
		final var baos = new ByteArrayOutputStream();
		assertEquals(payloadSize, c.transferTo(baos));
		compareFullCopy(baos.toByteArray());
		checkEOF();
	}

	@Test
	void testSkip() throws IOException {
		assertEquals(payloadSize, c.skip(payloadSize));
		checkEOF();
	}

	private void compareFullCopy(final byte[] result) {
		assertTrue(Arrays.equals(
				data, payloadPosition, payloadPosition + payloadSize,
				result, 0, result.length));
	}

	private void checkEOF() throws IOException {
		assertEquals(payloadPosition + payloadSize, channel.position());
		assertEquals(0, c.available());
	}

}
