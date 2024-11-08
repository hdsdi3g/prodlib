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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeaderTest.PREAMBLE_PAYLOAD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockOutputStreamChunkTest {

	@Mock
	FileChannel channel;
	@Fake(min = 10000, max = 1000000)
	long payloadPosition;
	@Fake(min = 10000, max = 1000000)
	long currentPos;
	@Fake
	byte oneByte;
	@Fake(min = 10, max = 100)
	byte[] data;

	@Captor
	ArgumentCaptor<ByteBuffer> byteBufferCaptor;

	DatablockOutputStreamChunk c;
	ByteBuffer buffer;

	@BeforeEach
	void init() throws Exception {
		when(channel.position()).thenReturn(payloadPosition);
		when(channel.write(any(ByteBuffer.class)))
				.then(f -> f.getArgument(0, ByteBuffer.class).remaining());
		c = new DatablockOutputStreamChunk(channel);
	}

	@AfterEach
	void end() throws IOException {
		verify(channel, atLeastOnce()).position();
	}

	@Test
	void testWriteInt() throws IOException {
		c.write(oneByte);

		verify(channel, times(1)).write(byteBufferCaptor.capture());
		buffer = byteBufferCaptor.getValue();
		assertEquals(oneByte, buffer.get());
		assertEquals(0, buffer.remaining());
	}

	@Test
	void testWriteByteArrayIntInt() throws IOException {
		c.write(data);

		verify(channel, times(1)).write(byteBufferCaptor.capture());
		buffer = byteBufferCaptor.getValue();

		final var writer = new byte[buffer.remaining()];
		buffer.get(writer);
		assertThat(data).isEqualTo(writer);
	}

	@Test
	void testWrite_noData() throws IOException {// NOSONAR S2699
		c.write(data, 0, 0);
	}

	@Test
	void testFlush() throws IOException {
		c.flush();
		verify(channel, times(1)).force(false);
	}

	@Test
	void testClose() throws IOException {
		currentPos += payloadPosition;
		when(channel.position()).thenReturn(currentPos);
		when(channel.write(any(ByteBuffer.class), eq(currentPos + PREAMBLE_PAYLOAD_SIZE))).thenReturn(4);

		c.close();

		verify(channel, times(1)).position(payloadPosition - CHUNK_HEADER_LEN);
		verify(channel, times(1)).position(currentPos);
		verify(channel, times(1)).write(byteBufferCaptor.capture(), eq(currentPos + PREAMBLE_PAYLOAD_SIZE));
		buffer = byteBufferCaptor.getValue();
		assertEquals(currentPos - payloadPosition, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

}
