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

public class DatablockDocumentBuilder {

	private final byte[] magicNumber;
	private final byte[] documentType;
	private final short typeVersion;
	private final int documentVersion;

	public DatablockDocumentBuilder(final byte[] magicNumber,
									final byte[] documentType,
									final short typeVersion,
									final int documentVersion) {
		this.magicNumber = magicNumber;
		this.documentType = documentType;
		this.typeVersion = typeVersion;
		this.documentVersion = documentVersion;
	}

	public static void write(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
		final var writed = channel.write(buffer);
		if (writed != buffer.capacity() || buffer.remaining() > 0) {
			throw new IOException("Can't write header: " + writed);
		}
	}

	public DatablockDocumentWriter createWriter(final WritableByteChannel channel) throws IOException {
		final var header = ByteBuffer.allocate(magicNumber.length
											   + documentType.length
											   + 2 /** typeVersion */
											   + 4 /** documentVersion */
		);

		header.put(magicNumber);
		header.put(documentType);
		header.putShort(typeVersion);
		header.putInt(documentVersion);
		header.flip();
		write(channel, header);
		return new DatablockDocumentWriter(channel);
	}

}
