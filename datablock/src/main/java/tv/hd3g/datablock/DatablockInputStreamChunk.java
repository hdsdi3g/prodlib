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

class DatablockInputStreamChunk extends InputStream {

	private final FileChannel channel;
	private final long payloadPosition;
	private final int payloadSize;
	private final ByteBuffer oneByteBuffer;

	DatablockInputStreamChunk(final FileChannel channel,
							  final long payloadPosition,
							  final int payloadSize) throws IOException {
		this.channel = Objects.requireNonNull(channel, "\"channel\" can't to be null");
		this.payloadPosition = payloadPosition;
		this.payloadSize = payloadSize;
		channel.position(payloadPosition);
		oneByteBuffer = ByteBuffer.allocate(1);
	}

	@Override
	public int read() throws IOException {
		if (available() <= 0) {
			return -1;
		}

		oneByteBuffer.clear();
		if (channel.read(oneByteBuffer) <= 0) {
			return -1;
		}

		oneByteBuffer.flip();
		return oneByteBuffer.get() & 0xFF;
	}

	@Override
	public long skip(final long n) throws IOException {
		final var avaliable = available();
		final var realSkip = min(n, avaliable);
		channel.position(channel.position() + realSkip);
		return realSkip;
	}

	@Override
	public int available() throws IOException {
		return payloadSize - (int) (channel.position() - payloadPosition);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		final var available = available();
		if (available <= 0) {
			return -1;
		}
		final var buffer = ByteBuffer.wrap(b, off, min(available, len));
		return channel.read(buffer);
	}

}
