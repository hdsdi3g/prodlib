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

import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeader.updateChunkHeaderPayloadSize;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DatablockOutputStreamChunk extends OutputStream implements IOTraits {

	private final FileChannel channel;
	private final long payloadPosition;
	private final ByteBuffer oneByteBuffer;

	public DatablockOutputStreamChunk(final FileChannel channel) throws IOException {
		this.channel = channel;
		payloadPosition = channel.position();
		oneByteBuffer = ByteBuffer.allocate(1);
	}

	@Override
	public void write(final int b) throws IOException {
		oneByteBuffer.clear();
		oneByteBuffer.put((byte) b);
		oneByteBuffer.flip();
		checkedWrite(channel, oneByteBuffer);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (len == 0) {
			return;
		}

		final var buffer = ByteBuffer.wrap(b, off, len);
		checkedWrite(channel, buffer);
	}

	@Override
	public void flush() throws IOException {
		channel.force(false);
	}

	@Override
	public void close() throws IOException {
		final var currentPos = channel.position();
		final var newPayloadSize = currentPos - payloadPosition;
		channel.position(payloadPosition - CHUNK_HEADER_LEN);
		updateChunkHeaderPayloadSize(channel, (int) newPayloadSize);
		channel.position(currentPos);
	}

}
