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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockChunkHeaderTest {

	@Fake
	String fourCCStr;
	@Fake(min = 4, max = 4)
	byte[] fourCC;
	@Fake
	short version;
	@Fake
	int payloadSize;

	DatablockChunkHeader h;

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testDatablockChunkHeaderByteArrayShortIntBoolean(final boolean archived) {
		h = new DatablockChunkHeader(fourCC, version, payloadSize, archived);

		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(payloadSize, h.getPayloadSize());
		assertEquals(archived, h.isArchived());
		assertFalse(h.isDeleted());
		final var now = System.currentTimeMillis();
		assertThat(h.getCreatedDate()).isBetween(now - 1000, now);

		final var tooBigFourcc = fourCCStr.getBytes();
		assertThrows(IllegalArgumentException.class,
				() -> new DatablockChunkHeader(tooBigFourcc, version, payloadSize, archived));
	}

	@Test
	void testDatablockChunkHeaderByteBuffer() {

	}

	@Test
	void testToByteBuffer() {

	}

	@Test
	void testUpdateChunkHeaderTags() {

	}

	@Test
	void testUpdateChunkHeaderPayloadSize() {

	}

	@Test
	void testToString() {

	}

}
