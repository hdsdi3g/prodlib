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
import static tv.hd3g.datablock.DatablockDocumentHeader.DOCUMENT_HEADER_LEN;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

// TODO add technical readme
public class DatablockDocument implements IOTraits {// TODO test

	private final FileChannel channel;
	private final ByteBuffer documentHeaderBuffer = ByteBuffer.allocate(DOCUMENT_HEADER_LEN);
	private final ByteBuffer chunkHeaderBuffer = ByteBuffer.allocate(CHUNK_HEADER_LEN);
	private final ByteBuffer chunkSeparator = ByteBuffer.allocate(1);

	public DatablockDocument(final FileChannel channel) throws IOException {
		this.channel = Objects.requireNonNull(channel, "\"channel\" can't to be null");
		if (channel.isOpen() == false) {
			throw new IOException("Can't works on closed channel");
		}
	}

	public synchronized DatablockDocumentHeader readDocumentHeader() throws IOException {
		documentHeaderBuffer.clear();
		checkIOSize(channel.read(documentHeaderBuffer, 0), documentHeaderBuffer);
		return new DatablockDocumentHeader(documentHeaderBuffer.flip().asReadOnlyBuffer());
	}

	public synchronized void writeDocumentHeader(final DatablockDocumentHeader header) throws IOException {
		final var buffer = header.toByteBuffer();
		checkIOSize(channel.write(buffer, 0), buffer);
	}

	/**
	 * @param chunkPayload No reset/flip will be done
	 */
	public synchronized void appendChunk(final DatablockChunkHeader chunkHeader,
										 final ByteBuffer chunkPayload) throws IOException {
		final var header = chunkHeader.toByteBuffer();
		checkIOSize(channel.write(header), header);
		checkIOSize(channel.write(chunkPayload), chunkPayload);

		chunkSeparator.clear();
		chunkSeparator.put(ZERO_BYTE);
		chunkSeparator.flip();
		checkIOSize(channel.write(chunkSeparator), chunkSeparator);
	}

	public synchronized void documentCrawl(final FoundedDataBlockDocumentChunk chunkCallback) throws IOException {
		channel.position(DOCUMENT_HEADER_LEN);

		while (channel.position() + 1l < channel.size()) {
			chunkHeaderBuffer.clear();
			checkIOSize(channel.read(chunkHeaderBuffer), chunkHeaderBuffer);
			final var header = new DatablockChunkHeader(chunkHeaderBuffer);
			final var chunkPayloadDocumentPosition = channel.position();

			final var currentChunkReader = new ChunkReader(chunkPayloadDocumentPosition, header.getPayloadSize());
			try {
				chunkCallback.onChunk(header, chunkPayloadDocumentPosition, currentChunkReader);
			} finally {
				currentChunkReader.clean();
			}

			final var nextChunkPosition = chunkPayloadDocumentPosition
										  + header.getPayloadSize();
			chunkSeparator.clear();
			checkIOSize(channel.read(chunkSeparator, nextChunkPosition), chunkSeparator);
			chunkSeparator.flip();
			checkEndBlank(chunkSeparator, chunkSeparator.capacity());
		}
	}

	public synchronized List<DataBlockChunkIndexItem> getDocumentMap() throws IOException {
		final var result = new ArrayList<DataBlockChunkIndexItem>();
		documentCrawl((chunkHeader,
					   chunkPayloadDocumentPosition,
					   payloadExtractor) -> result
							   .add(new DataBlockChunkIndexItem(chunkHeader, chunkPayloadDocumentPosition)));
		return result;
	}

	public synchronized void documentRefactor(final FileChannel newDocument,
											  final Function<DataBlockChunkIndexItem, DatablockMigrateChunkPolicy> keepChunkPolicy) throws IOException {
		final var targetDocument = new DatablockDocument(newDocument);
		newDocument.truncate(DOCUMENT_HEADER_LEN);
		newDocument.position(0);

		final var actualHeader = readDocumentHeader();
		targetDocument.writeDocumentHeader(actualHeader.getIncrementedDocumentVersion());

		final var actualPos = channel.position();

		documentCrawl((chunkHeader,
					   chunkPayloadDocumentPosition,
					   payloadExtractor) -> {
			final var policy = keepChunkPolicy.apply(
					new DataBlockChunkIndexItem(chunkHeader, chunkPayloadDocumentPosition));

			if (policy.keepActualChunk()) {
				final var startChunkPos = chunkPayloadDocumentPosition - CHUNK_HEADER_LEN;
				final var chunkLen = CHUNK_HEADER_LEN
									 + chunkHeader.getPayloadSize()
									 + chunkSeparator.capacity();

				try {
					channel.transferTo(startChunkPos, chunkLen, newDocument);
				} catch (final IOException e) {
					throw new UncheckedIOException(
							"Can't transfert to newDocument (readed from=" + startChunkPos + ", len=" + chunkLen + ")",
							e);
				}

			}

			if (policy.changeActualChunk()) {
				try {
					new ChunkReader(chunkPayloadDocumentPosition, chunkHeader.getPayloadSize())
							.updateHeader(
									policy.markActualChunkAsArchived(),
									policy.markActualChunkAsDeleted());
				} catch (final IOException e) {
					throw new UncheckedIOException("Can't write to actual document", e);
				}
			}

		});

		channel.position(actualPos);
	}

	class ChunkReader implements DatablockChunkPayloadExtractor {

		private final long position;
		private final long size;

		private MemorySegment currentMemorySegment;
		private Arena arena;

		/**
		 * Always call clean() after getCurrentChunkPayload()
		 */
		ChunkReader(final long position, final long size) {
			this.position = position;
			this.size = size;
		}

		@SuppressWarnings("preview")
		@Override
		public synchronized ByteBuffer getCurrentChunkPayload() throws IOException {
			arena = Arena.ofShared();
			currentMemorySegment = channel.map(READ_WRITE, position, size, arena);
			return currentMemorySegment.asByteBuffer();
		}

		@Override
		public synchronized byte[] getCurrentChunkPayloadBytes() throws IOException {
			final var result = new byte[(int) size];
			final var buffer = ByteBuffer.wrap(result);
			final var currentPos = channel.position();
			checkIOSize(channel.read(buffer, position), buffer);
			channel.position(currentPos);
			return result;
		}

		@Override
		public synchronized void updateHeader(final boolean setArchived,
											  final boolean setDeleted) throws IOException {
			final var currentPos = channel.position();
			channel.position(position);
			DatablockChunkHeader.updateHeader(channel, setArchived, setDeleted);
			channel.position(currentPos);
		}

		synchronized void clean() {
			if (currentMemorySegment == null || arena == null) {
				return;
			}
			try {
				currentMemorySegment.unload();
				arena.close();
			} finally {
				currentMemorySegment = null;
				arena = null;
			}
		}

	}

}
