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
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class DatablockDocumentSession implements AutoCloseable {

	private record DataBlock(byte[] fourCC,
							 short version,
							 ByteBuffer payload) {

	}

	private final LinkedBlockingQueue<DataBlock> blocks;
	private final WritableByteChannel channel;
	private final byte[] fourCC;
	private final short version;

	public DatablockDocumentSession(final WritableByteChannel channel,
									final byte[] fourCC,
									final short version) {
		this.channel = channel;
		this.fourCC = fourCC;
		this.version = version;
		blocks = new LinkedBlockingQueue<>();
	}

	public void addNextDataBlock(final byte[] fourCC,
								 final short version,
								 final ByteBuffer payload) throws IOException {
		blocks.add(new DataBlock(fourCC, version, payload));
	}

	@Override
	public void close() throws Exception {
		final var buffer = ByteBuffer.allocate(4 + 2 + 4);
		buffer.put(fourCC, 0, 4);
		buffer.putShort(version);

		final var totalSize = blocks.stream()
				.map(DataBlock::payload)
				.mapToInt(ByteBuffer::remaining)
				.map(r -> r + 4 + 2 + 4 + 1)
				.sum();
		buffer.putInt(totalSize);
		buffer.flip();
		channel.write(buffer);

		for (final var entry : blocks) {
			buffer.clear();
			buffer.put(entry.fourCC, 0, 4);
			buffer.putShort(entry.version);
			buffer.putInt(entry.payload.remaining());
			buffer.flip();
			channel.write(buffer);

			channel.write(entry.payload);

			buffer.clear();
			buffer.put((byte) 0);
			buffer.flip();
			channel.write(buffer);
		}

		buffer.flip();
		channel.write(buffer);
	}

}
