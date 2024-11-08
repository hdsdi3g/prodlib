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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class DatablockChunkHeader implements IOTraits {// TODO debug tools

	public static final int FOURCC_EXPECTED_SIZE = 4;
	public static final int BLANK_EXPECTED_SIZE = 13;
	public static final int CHUNK_HEADER_LEN = FOURCC_EXPECTED_SIZE
											   + 2 /** version */
											   + 4 /** payloadSize */
											   + 8 /** createdDate */
											   + 1 /** deleted/archived */
											   + BLANK_EXPECTED_SIZE;

	public static final byte BYTE_TAG_DELETED = 0x01;
	public static final byte BYTE_TAG_ARCHIVED = 0x02;

	private final byte[] fourCC;
	private final short version;
	private final int payloadSize;
	private final long createdDate;
	private final boolean archived;
	private final boolean deleted;

	/**
	 * @param fourCC 4 bytes to identify and route to process the chunk
	 * @param version chunk type version
	 * @param payloadSize data payload payloadSize
	 * @param compressed is payload is compressed
	 * @param crc payload crc result
	 * @param archived marked as archived
	 */
	DatablockChunkHeader(final byte[] fourCC,
						 final short version,
						 final int payloadSize,
						 final boolean archived) {
		if (fourCC.length != FOURCC_EXPECTED_SIZE) {
			throw new IllegalArgumentException("fourCC len must equals "
											   + FOURCC_EXPECTED_SIZE + " bytes");
		}
		this.fourCC = fourCC;
		this.version = version;
		this.payloadSize = payloadSize;
		createdDate = System.currentTimeMillis();
		deleted = false;
		this.archived = archived;
	}

	DatablockChunkHeader(final ByteBuffer readFrom) {
		checkRemaining(readFrom, CHUNK_HEADER_LEN);
		fourCC = new byte[FOURCC_EXPECTED_SIZE];
		readFrom.get(fourCC);

		version = readFrom.getShort();
		payloadSize = readFrom.getInt();
		createdDate = readFrom.getLong();

		final var flag = readFrom.get();
		deleted = (flag & BYTE_TAG_DELETED) == BYTE_TAG_DELETED;
		archived = (flag & BYTE_TAG_ARCHIVED) == BYTE_TAG_ARCHIVED;

		checkEndBlank(readFrom, BLANK_EXPECTED_SIZE);
	}

	ByteBuffer toByteBuffer() {
		final var header = ByteBuffer.allocate(CHUNK_HEADER_LEN);
		header.put(fourCC);
		header.putShort(version);
		header.putInt(payloadSize);
		header.putLong(createdDate);

		final var flag = getFlag(deleted, archived);
		header.put(flag);
		header.put(new byte[BLANK_EXPECTED_SIZE]);
		header.flip();
		return header.asReadOnlyBuffer();
	}

	private static byte getFlag(final boolean deleted, final boolean archived) {
		return (byte) ((deleted ? BYTE_TAG_DELETED : ZERO_BYTE)
					   + (archived ? BYTE_TAG_ARCHIVED : ZERO_BYTE));
	}

	/**
	 * @param channel pos must be set on the first chunk header byte
	 */
	static void updateChunkHeaderTags(final FileChannel channel,
									  final boolean setArchived,
									  final boolean setDeleted) throws IOException {
		final var buffer = ByteBuffer.wrap(new byte[] { getFlag(setDeleted, setArchived) });

		IOTraits.checkIOSize(channel.write(
				buffer,
				channel.position()
						+ FOURCC_EXPECTED_SIZE
						+ 2 /** version */
						+ 4 /** payloadSize */
						+ 8 /** createdDate */
		), buffer.capacity());
	}

	/**
	 * @param channel pos must be set on the first chunk header byte
	 */
	static void updateChunkHeaderPayloadSize(final FileChannel channel,
											 final int newPayloadSize) throws IOException {
		final var buffer = ByteBuffer.allocate(4 /** payloadSize */
		);
		buffer.putInt(newPayloadSize);
		buffer.flip();

		IOTraits.checkIOSize(channel.write(
				buffer,
				channel.position()
						+ FOURCC_EXPECTED_SIZE
						+ 2 /** version */
		), buffer.capacity());
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("DatablockChunkHeader [fourCC=");
		builder.append(encodeHexString(fourCC));
		builder.append(", version=");
		builder.append(version);
		builder.append(", payloadSize=");
		builder.append(payloadSize);
		builder.append(", createdDate=");
		builder.append(new Date(createdDate));
		builder.append(", archived=");
		builder.append(archived);
		builder.append(", deleted=");
		builder.append(deleted);
		builder.append("]");
		return builder.toString();
	}

}
