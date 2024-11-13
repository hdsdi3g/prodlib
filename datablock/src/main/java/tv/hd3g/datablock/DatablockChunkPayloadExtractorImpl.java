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

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeader.updateChunkHeaderTags;
import static tv.hd3g.datablock.NIOUtils.checkedRead;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.Optional;

class DatablockChunkPayloadExtractorImpl implements DatablockChunkPayloadExtractor {

	private final FileChannel channel;
	private final long payloadPosition;
	private final int payloadSize;

	private MemorySegment currentMemorySegment;
	private Arena arena;

	/**
	 * Always call clean() after getCurrentChunkPayload()
	 */
	DatablockChunkPayloadExtractorImpl(final FileChannel channel,
									   final long payloadPosition,
									   final int payloadSize) {
		this.channel = Objects.requireNonNull(channel, "\"channel\" can't to be null");
		this.payloadPosition = payloadPosition;
		this.payloadSize = payloadSize;
	}

	@SuppressWarnings("preview")
	@Override
	public synchronized ByteBuffer getCurrentChunkPayload() throws IOException {
		clean();
		arena = Arena.ofShared();
		currentMemorySegment = channel.map(READ_WRITE, payloadPosition, payloadSize, arena);
		return currentMemorySegment.asByteBuffer();
	}

	@Override
	public synchronized byte[] getCurrentChunkPayloadBytes() throws IOException {
		final var result = new byte[payloadSize];
		final var buffer = ByteBuffer.wrap(result);
		final var currentPos = channel.position();
		checkedRead(channel, payloadPosition, buffer);
		channel.position(currentPos);
		return result;
	}

	@Override
	public synchronized void updateHeader(final boolean setArchived,
										  final boolean setDeleted) throws IOException {
		final var currentPos = channel.position();
		channel.position(payloadPosition - CHUNK_HEADER_LEN);
		updateChunkHeaderTags(channel, setArchived, setDeleted);
		channel.position(currentPos);
	}

	@Override
	public synchronized <T> T createInputStream(final InputStreamFunction<T> chunkReader) throws IOException {
		final var currentPos = channel.position();
		T result;
		try (final var inputStream = new DatablockInputStreamChunk(channel, payloadPosition, payloadSize)) {
			result = chunkReader.chunkReader(inputStream);
		} finally {
			channel.position(currentPos);
		}
		return result;
	}

	synchronized void clean() {
		if (currentMemorySegment == null && arena == null) {
			return;
		}
		try {
			Optional.ofNullable(currentMemorySegment)
					.ifPresent(MemorySegment::unload);
		} finally {
			currentMemorySegment = null;
			try {
				Optional.ofNullable(arena)
						.ifPresent(Arena::close);
			} finally {
				arena = null;
			}
		}
	}

}
