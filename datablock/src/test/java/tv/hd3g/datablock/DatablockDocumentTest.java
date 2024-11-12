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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeader.FOURCC_EXPECTED_SIZE;
import static tv.hd3g.datablock.DatablockDocument.CHUNK_SEPARATOR_SIZE;
import static tv.hd3g.datablock.DatablockDocumentHeader.DOCUMENT_HEADER_LEN;
import static tv.hd3g.datablock.IOTraits.ZERO_BYTE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockDocumentTest extends RealFileWork {

	@Fake(min = 8, max = 8)
	byte[] magicNumber;
	@Fake(min = 8, max = 8)
	byte[] documentType;
	@Fake
	short typeVersion;
	@Fake
	int documentVersion;

	@Fake(min = FOURCC_EXPECTED_SIZE, max = FOURCC_EXPECTED_SIZE)
	byte[] fourCC;
	@Fake
	short version;
	@Fake
	boolean archived;

	DatablockDocumentHeader ddh;

	DatablockDocument d;

	@BeforeEach
	void init() throws Exception {
		channel = FileChannel.open(file.toPath(), CREATE, READ, WRITE);
		channel.truncate(0);
		d = new DatablockDocument(channel);
		ddh = new DatablockDocumentHeader(magicNumber, documentType, typeVersion, documentVersion);
	}

	@Test
	void testDatablockDocument_closed() throws IOException {
		channel.close();
		assertThrows(IOException.class, () -> new DatablockDocument(channel));
	}

	@Test
	void testGetDocumentHeader_emptyFile() {
		assertThrows(IOException.class, () -> d.getDocumentHeader());
	}

	@Test
	void testPutDocumentHeader() throws IOException {
		assertEquals(0, file.length());
		assertEquals(0, channel.position());

		d.putDocumentHeader(ddh);

		assertEquals(DOCUMENT_HEADER_LEN, file.length());
		assertEquals(DOCUMENT_HEADER_LEN, channel.position());
	}

	@Test
	void testGetDocumentHeader() throws IOException {
		d.putDocumentHeader(ddh);
		d = new DatablockDocument(channel);
		assertEquals(ddh, d.getDocumentHeader());
		assertEquals(DOCUMENT_HEADER_LEN, file.length());
		assertEquals(DOCUMENT_HEADER_LEN, channel.position());
	}

	@Test
	void testAppendChunk_byteBuffer() throws IOException {
		d.appendChunk(fourCC, version, archived, ByteBuffer.wrap(data));
		checkWritedChunk();
	}

	@Test
	void testAppendChunk_outputStream() throws IOException {
		d.appendChunk(fourCC, version, archived, writer -> writer.write(data));
		checkWritedChunk();
	}

	@Test
	void testAppendEmptyChunk() throws IOException {
		Arrays.fill(data, ZERO_BYTE);
		d.appendEmptyChunk(fourCC, version, archived, data.length);
		checkWritedChunk();
	}

	@Test
	void testDocumentCrawl() {
		// XXX
	}

	@Test
	void testGetDocumentMap() {
		// XXX

	}

	@Test
	void testDocumentRefactor() {
		// XXX

	}

	// XXX createChunkPayloadExtractor don't need to test: tested internally (see coverage)

	private void checkWritedChunk() throws IOException {
		assertEquals(CHUNK_HEADER_LEN + data.length + CHUNK_SEPARATOR_SIZE, file.length());
		assertEquals(file.length(), channel.position());

		channel.position(0);
		final var realData = readFileToByteArray(file);

		assertTrue(Arrays.equals(
				data, 0, data.length,
				realData, CHUNK_HEADER_LEN, CHUNK_HEADER_LEN + data.length));

		final var chunkHeaderByteBuffer = ByteBuffer.wrap(realData, 0, CHUNK_HEADER_LEN);
		final var h = new DatablockChunkHeader(chunkHeaderByteBuffer);

		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(data.length, h.getPayloadSize());
		assertEquals(archived, h.isArchived());
		assertFalse(h.isDeleted());
		final var now = System.currentTimeMillis();
		assertThat(h.getCreatedDate()).isBetween(now - 1000, now);

		final var zeros = new byte[CHUNK_SEPARATOR_SIZE];
		assertTrue(Arrays.equals(
				zeros, 0, zeros.length,
				realData, CHUNK_HEADER_LEN + data.length, (int) file.length()));

	}

}
