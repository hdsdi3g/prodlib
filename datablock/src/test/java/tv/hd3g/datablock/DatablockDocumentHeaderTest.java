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

import static java.util.Arrays.fill;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tv.hd3g.datablock.DatablockDocumentHeader.DOCUMENT_HEADER_LEN;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockDocumentHeaderTest {

	@Fake(min = 8, max = 8)
	byte[] magicNumber;
	@Fake(min = 8, max = 8)
	byte[] documentType;
	@Fake
	short typeVersion;
	@Fake
	int documentVersion;

	DatablockDocumentHeader h;

	@BeforeEach
	void init() {
		h = new DatablockDocumentHeader(magicNumber, documentType, typeVersion, documentVersion);
		checkConsts(h);
		assertEquals(documentVersion, h.getDocumentVersion());
	}

	@Test
	void testInvalidConstructDataSize_magicNumber() {
		final var newMagicNumber = new byte[0];
		assertThrows(IllegalArgumentException.class,
				() -> new DatablockDocumentHeader(newMagicNumber, documentType, typeVersion, documentVersion));
	}

	@Test
	void testInvalidConstructDataSize_documentType() {
		final var newDocumentType = new byte[0];
		assertThrows(IllegalArgumentException.class,
				() -> new DatablockDocumentHeader(magicNumber, newDocumentType, typeVersion, documentVersion));
	}

	@Test
	void testGetIncrementedDocumentVersion() {
		final var incremented = h.getIncrementedDocumentVersion();
		checkConsts(incremented);
		assertEquals(documentVersion + 1, incremented.getDocumentVersion());
	}

	@Test
	void testImpExByteBuffer() {
		final var bb = h.toByteBuffer();
		assertNotNull(bb);
		assertEquals(0, bb.position());
		assertEquals(DOCUMENT_HEADER_LEN, bb.capacity());
		assertEquals(DOCUMENT_HEADER_LEN, bb.remaining());

		h = new DatablockDocumentHeader(bb);
		checkConsts(h);
		assertEquals(documentVersion, h.getDocumentVersion());

		assertEquals(DOCUMENT_HEADER_LEN, bb.position());
		assertEquals(0, bb.remaining());
	}

	@Test
	void testImpByteBuffer_invalidData() {
		final var content = new byte[DOCUMENT_HEADER_LEN];
		fill(content, (byte) 1);
		final var bb = ByteBuffer.wrap(content);

		assertThrows(IllegalArgumentException.class,
				() -> new DatablockDocumentHeader(bb));
	}

	@Test
	void testToString() {
		assertThat(h.toString()).contains(
				encodeHexString(magicNumber),
				encodeHexString(documentType),
				String.valueOf(typeVersion),
				String.valueOf(documentVersion));
	}

	private void checkConsts(final DatablockDocumentHeader compareTo) {
		assertArrayEquals(magicNumber, compareTo.getMagicNumber());
		assertArrayEquals(documentType, compareTo.getDocumentType());
		assertEquals(typeVersion, compareTo.getTypeVersion());
	}

}
