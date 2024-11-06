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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class IOTraitsTest {

	class TestIOTraits implements IOTraits {

	}

	@Fake(min = 10, max = 10000)
	int operation;
	@Fake(min = 10, max = 10000)
	int expect;
	@Fake(min = 10, max = 10000)
	long pos;

	@Mock
	WritableByteChannel writableByteChannel;
	@Mock
	ReadableByteChannel readableByteChannel;
	@Mock
	FileChannel fileChannel;

	ByteBuffer readFrom;
	TestIOTraits t = new TestIOTraits();

	@Test
	void testCheckIOSizeIntInt() throws IOException {
		assertThrows(IOException.class, () -> IOTraits.checkIOSize(operation, expect));
		IOTraits.checkIOSize(expect, expect);
	}

	@Test
	void testCheckedWriteWritableByteChannelByteBuffer() throws IOException {
		readFrom = ByteBuffer.allocate(operation);

		when(writableByteChannel.write(readFrom)).thenReturn(operation);
		t.checkedWrite(writableByteChannel, readFrom);
		verify(writableByteChannel, times(1)).write(readFrom);
	}

	@Test
	void testCheckedWriteFileChannelLongByteBuffer() throws IOException {
		readFrom = ByteBuffer.allocate(operation);

		when(fileChannel.write(readFrom, pos)).thenReturn(operation);
		t.checkedWrite(fileChannel, pos, readFrom);
		verify(fileChannel, times(1)).write(readFrom, pos);
	}

	@Test
	void testCheckedReadReadableByteChannelByteBuffer() throws IOException {
		readFrom = ByteBuffer.allocate(operation);

		when(readableByteChannel.read(readFrom)).thenReturn(operation);
		t.checkedRead(readableByteChannel, readFrom);
		verify(readableByteChannel, times(1)).read(readFrom);
	}

	@Test
	void testCheckedReadFileChannelLongByteBuffer() throws IOException {
		readFrom = ByteBuffer.allocate(operation);

		when(fileChannel.read(readFrom, pos)).thenReturn(operation);
		t.checkedRead(fileChannel, pos, readFrom);
		verify(fileChannel, times(1)).read(readFrom, pos);
	}

	@Test
	void testCheckEndBlank() {
		readFrom = ByteBuffer.allocate(operation * 2);
		readFrom.limit(readFrom.capacity());
		t.checkEndBlank(readFrom, operation);
		readFrom.clear();

		readFrom.put(operation / 2, new byte[] { (byte) 0x01 });
		readFrom.position(0);
		readFrom.limit(readFrom.capacity());

		assertThrows(IllegalArgumentException.class, () -> t.checkEndBlank(readFrom, operation));
		assertThrows(IllegalArgumentException.class, () -> t.checkEndBlank(readFrom, operation * 2 + 1));
	}

	@Test
	void testCheckRemaining() {
		readFrom = ByteBuffer.allocate(operation);

		t.checkRemaining(readFrom, operation);
		assertThrows(IllegalArgumentException.class, () -> t.checkRemaining(readFrom, operation + 1));
		readFrom.get();
		assertThrows(IllegalArgumentException.class, () -> t.checkRemaining(readFrom, operation));
	}

}
