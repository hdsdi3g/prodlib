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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public interface IOTraits {

	String BYTES_STR = " bytes";
	byte ZERO_BYTE = 0x0;

	static void checkIOSize(final int operation, final int expect) throws IOException {
		if (operation != expect) {
			throw new IOException("Invalid I/O operation: expect " + expect + " and get " + operation);
		}
	}

	default void checkedWrite(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
		checkIOSize(channel.write(buffer), buffer.capacity());
	}

	default void checkedWrite(final FileChannel channel,
							  final long position,
							  final ByteBuffer buffer) throws IOException {
		checkIOSize(channel.write(buffer, position), buffer.capacity());
	}

	default void checkedRead(final ReadableByteChannel channel, final ByteBuffer buffer) throws IOException {
		checkIOSize(channel.read(buffer), buffer.capacity());
	}

	default void checkedRead(final FileChannel channel,
							 final long position,
							 final ByteBuffer buffer) throws IOException {
		checkIOSize(channel.read(buffer, position), buffer.capacity());
	}

	default void checkEndBlank(final ByteBuffer readFrom, final int blankExpectedSize) {
		if (readFrom.limit() < blankExpectedSize) {
			throw new IllegalArgumentException("Invalid limit space: " + readFrom.limit() + "/" + blankExpectedSize);
		}

		for (var pos = 0; pos < blankExpectedSize; pos++) {
			if (readFrom.get() != ZERO_BYTE) {
				throw new IllegalArgumentException("Invalid blank space");
			}
		}
	}

	default void checkRemaining(final ByteBuffer readFrom, final int headerLen) {
		if (readFrom.remaining() < headerLen) {
			throw new IllegalArgumentException("Not enough remaining space (" + readFrom.remaining()
											   + " bytes) to read from buffer. It need at least "
											   + headerLen + BYTES_STR);
		}
	}

}
