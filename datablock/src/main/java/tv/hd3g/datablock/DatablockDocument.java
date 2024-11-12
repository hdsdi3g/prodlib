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
import static tv.hd3g.datablock.DatablockDocumentHeader.DOCUMENT_HEADER_LEN;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO add technical readme
// TODO add v1 + object storage + defrag
// TODO add indexed list
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
		checkedRead(channel, 0, documentHeaderBuffer);
		return new DatablockDocumentHeader(documentHeaderBuffer.flip().asReadOnlyBuffer());
	}

	/*
	 * TODO needed ?
	 * @return newDocumentVersion
	 */
	/*public synchronized int incrementDocumentVersion() throws IOException {
		final var buffer = ByteBuffer.allocate(4 /** documentVersion *
		);

		checkedRead(channel, DOCUMENT_VERSION_POS, buffer);
		buffer.flip();
		final var newDocumentVersion = buffer.getInt() + 1;
		buffer.reset();
		buffer.putInt(newDocumentVersion);
		buffer.flip();
		checkedWrite(channel, DOCUMENT_VERSION_POS, buffer);
		return newDocumentVersion;
	}*/

	public synchronized void writeDocumentHeader(final DatablockDocumentHeader header) throws IOException {
		final var buffer = header.toByteBuffer();
		checkedWrite(channel, 0, buffer);
	}

	/**
	 * @param chunkPayload No reset/flip will be done
	 */
	public synchronized void appendChunk(final byte[] fourCC,
										 final short version,
										 final boolean archived,
										 final ByteBuffer chunkPayload) throws IOException {
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, chunkPayload.remaining(), archived);

		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);
		checkedWrite(channel, chunkPayload);

		chunkSeparator.clear();
		chunkSeparator.put(ZERO_BYTE);
		chunkSeparator.flip();
		checkedWrite(channel, chunkSeparator);
	}

	public synchronized void appendChunk(final byte[] fourCC,
										 final short version,
										 final boolean archived,
										 final Consumer<OutputStream> writer) throws IOException {
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, 0, archived);
		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);

		try (var outputStream = new DatablockOutputStreamChunk(channel)) {
			writer.accept(outputStream);
		} finally {
			chunkSeparator.clear();
			chunkSeparator.put(ZERO_BYTE);
			chunkSeparator.flip();
			checkedWrite(channel, chunkSeparator);
		}
	}

	public synchronized DataBlockChunkIndexItem appendEmptyChunk(final byte[] fourCC,
																 final short version,
																 final boolean archived,
																 final int payloadSize) throws IOException {
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, payloadSize, archived);
		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);

		final var payloadPosition = channel.position();
		channel.position(payloadPosition + payloadSize);

		chunkSeparator.clear();
		chunkSeparator.put(ZERO_BYTE);
		chunkSeparator.flip();
		checkedWrite(channel, chunkSeparator);

		return new DataBlockChunkIndexItem(chunkHeader, payloadPosition);
	}

	public synchronized void documentCrawl(final FoundedDataBlockDocumentChunk chunkCallback) throws IOException {
		channel.position(DOCUMENT_HEADER_LEN);

		while (channel.position() + 1l < channel.size()) {
			chunkHeaderBuffer.clear();
			checkedRead(channel, chunkHeaderBuffer);
			final var header = new DatablockChunkHeader(chunkHeaderBuffer);
			final var chunkPayloadDocumentPosition = channel.position();

			final var currentChunkReader = new DatablockChunkPayloadExtractorImpl(
					channel, chunkPayloadDocumentPosition, header.getPayloadSize());
			try {
				chunkCallback.onChunk(header, chunkPayloadDocumentPosition, currentChunkReader);
			} finally {
				currentChunkReader.clean();
			}

			final var nextChunkPosition = chunkPayloadDocumentPosition
										  + header.getPayloadSize();
			chunkSeparator.clear();
			checkedRead(channel, nextChunkPosition, chunkSeparator);
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
					new DatablockChunkPayloadExtractorImpl(
							channel, chunkPayloadDocumentPosition, chunkHeader.getPayloadSize())
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

	DatablockChunkPayloadExtractorImpl createChunkPayloadExtractor(final long payloadPosition, final int payloadSize) {
		return new DatablockChunkPayloadExtractorImpl(channel, payloadPosition, payloadSize);
	}

}
