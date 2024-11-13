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

import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.datablock.DatablockChunkHeader.BLANK_EXPECTED_SIZE;
import static tv.hd3g.datablock.DatablockChunkHeader.BYTE_TAG_ARCHIVED;
import static tv.hd3g.datablock.DatablockChunkHeader.BYTE_TAG_DELETED;
import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeader.FOURCC_EXPECTED_SIZE;
import static tv.hd3g.datablock.DatablockChunkHeader.updateChunkHeaderPayloadSize;
import static tv.hd3g.datablock.DatablockChunkHeader.updateChunkHeaderTags;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockChunkHeaderTest {

	static final int PREAMBLE_PAYLOAD_SIZE = FOURCC_EXPECTED_SIZE
											 + 2 /** version */
	;
	private static final int PREAMBLE_TAGS = FOURCC_EXPECTED_SIZE
											 + 2 /** version */
											 + 4 /** payloadSize */
											 + 8 /** createdDate */
	;

	@Fake
	String fourCCStr;
	@Fake(min = FOURCC_EXPECTED_SIZE, max = FOURCC_EXPECTED_SIZE)
	byte[] fourCC;
	@Fake
	short version;
	@Fake
	int payloadSize;
	@Fake
	long createdDate;
	@Mock
	FileChannel channel;
	@Fake(min = 1000, max = 100000)
	long channelPosition;

	@Captor
	ArgumentCaptor<ByteBuffer> byteBufferCaptor;

	DatablockChunkHeader h;

	@BeforeEach
	void init() throws Exception {
		when(channel.write(any(ByteBuffer.class), anyLong()))
				.then(f -> f.getArgument(0, ByteBuffer.class).remaining());
		when(channel.position()).thenReturn(channelPosition);
	}

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

	private ByteBuffer makeFullBuffer() {
		return makeFullBuffer(false, false);
	}

	private ByteBuffer makeFullBuffer(final boolean deleted, final boolean archived) {
		final var readFrom = ByteBuffer.allocate(CHUNK_HEADER_LEN);
		readFrom.put(fourCC);
		readFrom.putShort(version);
		readFrom.putInt(payloadSize);
		readFrom.putLong(createdDate);
		if (deleted && archived) {
			readFrom.put((byte) (BYTE_TAG_DELETED + BYTE_TAG_ARCHIVED));
		} else if (archived) {
			readFrom.put(BYTE_TAG_ARCHIVED);
		} else if (deleted) {
			readFrom.put(BYTE_TAG_DELETED);
		} else {
			readFrom.put((byte) 0);
		}
		readFrom.put(new byte[BLANK_EXPECTED_SIZE]);
		assertEquals(0, readFrom.remaining());
		readFrom.flip();
		return readFrom;
	}

	@Test
	void testDatablockChunkHeaderByteBuffer() {
		final var readFrom = makeFullBuffer();
		h = new DatablockChunkHeader(readFrom);
		assertEquals(0, readFrom.remaining());

		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(payloadSize, h.getPayloadSize());
		assertEquals(createdDate, h.getCreatedDate());
		assertFalse(h.isArchived());
		assertFalse(h.isDeleted());

	}

	@Test
	void testDatablockChunkHeaderByteBuffer_delete() {
		final var readFrom = makeFullBuffer();
		readFrom.put(PREAMBLE_TAGS, BYTE_TAG_DELETED);
		readFrom.position(0);
		readFrom.limit(readFrom.capacity());
		h = new DatablockChunkHeader(readFrom);
		assertFalse(h.isArchived());
		assertTrue(h.isDeleted());
	}

	@Test
	void testDatablockChunkHeaderByteBuffer_archived() {
		final var readFrom = makeFullBuffer();
		readFrom.put(PREAMBLE_TAGS, BYTE_TAG_ARCHIVED);
		readFrom.position(0);
		readFrom.limit(readFrom.capacity());
		h = new DatablockChunkHeader(readFrom);
		assertTrue(h.isArchived());
		assertFalse(h.isDeleted());
	}

	@Test
	void testDatablockChunkHeaderByteBuffer_deleted_archived() {
		final var readFrom = makeFullBuffer();
		readFrom.put(PREAMBLE_TAGS, (byte) (BYTE_TAG_DELETED + BYTE_TAG_ARCHIVED));
		readFrom.position(0);
		readFrom.limit(readFrom.capacity());
		h = new DatablockChunkHeader(readFrom);
		assertTrue(h.isArchived());
		assertTrue(h.isDeleted());
	}

	@Test
	void testDatablockChunkHeaderByteBuffer_tooSmall() {
		final var readFrom = makeFullBuffer();
		readFrom.limit(readFrom.capacity() - 1);
		assertThrows(IllegalArgumentException.class, () -> new DatablockChunkHeader(readFrom));

	}

	@Test
	void testDatablockChunkHeaderByteBuffer_notZeroEnds() {
		final var readFrom = makeFullBuffer();
		readFrom.limit(readFrom.capacity());
		readFrom.put(readFrom.capacity() - 1, (byte) 1);
		readFrom.position(0);
		readFrom.limit(readFrom.capacity());
		assertThrows(IllegalArgumentException.class, () -> new DatablockChunkHeader(readFrom));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testToByteBuffer(final boolean archived) {
		h = new DatablockChunkHeader(fourCC, version, payloadSize, archived);
		final var buffer = h.toByteBuffer();

		assertEquals(CHUNK_HEADER_LEN, buffer.remaining());
		assertEquals(CHUNK_HEADER_LEN, buffer.capacity());
		assertEquals(0, buffer.position());

		final var dataActual = new byte[CHUNK_HEADER_LEN];
		buffer.get(dataActual);

		createdDate = h.getCreatedDate();
		final var dataExpected = new byte[CHUNK_HEADER_LEN];
		final var readFrom = makeFullBuffer(false, archived);
		readFrom.get(dataExpected);

		assertThat(dataActual).isEqualTo(dataExpected);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testToByteBuffer_deleted(final boolean archived) {
		final var initBuffer = makeFullBuffer(true, archived);
		initBuffer.position(0);
		initBuffer.limit(initBuffer.capacity());
		final var buffer = new DatablockChunkHeader(initBuffer).toByteBuffer();

		final var dataActual = new byte[CHUNK_HEADER_LEN];
		buffer.get(dataActual);

		final var dataExpected = new byte[CHUNK_HEADER_LEN];
		final var readFrom = makeFullBuffer(true, archived);
		readFrom.get(dataExpected);

		assertThat(dataActual).isEqualTo(dataExpected);
	}

	@ParameterizedTest
	@MethodSource("tv.hd3g.commons.testtools.MockToolsExtendsJunit#provide2Booleans")
	void testUpdateChunkHeaderTags(final boolean setDeleted, final boolean setArchived) throws IOException {
		updateChunkHeaderTags(channel, setArchived, setDeleted);
		verify(channel, times(1))
				.write(byteBufferCaptor.capture(), eq(channelPosition + PREAMBLE_TAGS));
		verify(channel, atLeastOnce()).position();

		final var buffer = byteBufferCaptor.getValue();
		assertEquals(0, buffer.position());
		assertEquals(1, buffer.remaining());

		final var reference = makeFullBuffer(setDeleted, setArchived).position(PREAMBLE_TAGS);
		assertEquals(reference.get(), buffer.get());
	}

	@Test
	void testUpdateChunkHeaderPayloadSize() throws IOException {
		updateChunkHeaderPayloadSize(channel, payloadSize);

		verify(channel, times(1))
				.write(byteBufferCaptor.capture(), eq(channelPosition + PREAMBLE_PAYLOAD_SIZE));
		verify(channel, atLeastOnce()).position();

		final var buffer = byteBufferCaptor.getValue();
		assertEquals(0, buffer.position());
		assertEquals(4, buffer.remaining());
		assertEquals(payloadSize, buffer.getInt());
	}

	@ParameterizedTest
	@MethodSource("tv.hd3g.commons.testtools.MockToolsExtendsJunit#provide2Booleans")
	void testToString(final boolean deleted, final boolean archived) {
		final var str = new DatablockChunkHeader(makeFullBuffer(deleted, archived)).toString();
		assertThat(str)
				.contains(encodeHexString(fourCC),
						String.valueOf(version),
						String.valueOf(payloadSize),
						new Date(createdDate).toString(),
						"archived=" + archived,
						"deleted=" + deleted);
	}

}
