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

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

class DatablockInputStreamChunk extends InputStream {// TODO test

	/** 1MB */
	public static final int BUFFER_SIZE = 0xFFFFF;
	private final FileChannel channel;
	private final long payloadPosition;
	private final int payloadSize;
	private final ByteBuffer buffer;

	DatablockInputStreamChunk(final FileChannel channel,
							  final long payloadPosition,
							  final int payloadSize) throws IOException {
		this.channel = Objects.requireNonNull(channel, "\"channel\" can't to be null");
		this.payloadPosition = payloadPosition;
		this.payloadSize = payloadSize;
		channel.position(payloadPosition);
		buffer = ByteBuffer.allocate(min(payloadSize, BUFFER_SIZE));
	}

	private boolean readNextBuffer() throws IOException {
		if (buffer.hasRemaining() == false) {
			final var available = getCurrentAvailable();
			if (available == 0) {
				return false;
			}
			buffer.clear();
			buffer.limit(min(buffer.capacity(), available));
			final var readed = channel.read(buffer);
			if (readed == 0) {
				return false;
			}
			buffer.flip();
		}
		return true;
	}

	@Override
	public int read() throws IOException {
		if (readNextBuffer() == false) {
			return -1;
		}
		return buffer.get() & 0xFF;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		var pos = off;
		var writeRemain = len;

		while (writeRemain > 0) {
			if (readNextBuffer() == false) {
				break;
			}

			final var ioSize = min(writeRemain, buffer.remaining());
			buffer.get(b, pos, ioSize);

			pos += ioSize;
			writeRemain -= ioSize;
		}

		return pos - off;

	}

	@Override
	public void close() throws IOException {
		buffer.clear();
	}

	private int getCurrentPosition() throws IOException {
		return (int) (channel.position() - payloadPosition);
	}

	private int getCurrentAvailable() throws IOException {
		return payloadSize - getCurrentPosition();
	}

}
