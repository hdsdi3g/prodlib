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

public class DatablockDocumentWriter {

	private final WritableByteChannel channel;

	public DatablockDocumentWriter(final WritableByteChannel channel) {
		this.channel = channel;
	}

	public void addNextDataBlock(final byte[] fourCC,
								 final short version,
								 final ByteBuffer payload) throws IOException {
		final var buffer = ByteBuffer.allocate(4 + 2 + 4);
		buffer.put(fourCC, 0, 4);
		buffer.putShort(version);
		buffer.putInt(payload.remaining());
		buffer.flip();
		channel.write(buffer);

		channel.write(payload);

		buffer.clear();
		buffer.put((byte) 0);
		buffer.flip();
		channel.write(buffer);
	}

	public DatablockDocumentSession createBlock(final byte[] fourCC,
												final short version) {
		return new DatablockDocumentSession(channel, fourCC, version);
	}

}
