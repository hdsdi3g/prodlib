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
import static tv.hd3g.datablock.NIOUtils.ZERO_BYTE;
import static tv.hd3g.datablock.NIOUtils.checkEndBlank;
import static tv.hd3g.datablock.NIOUtils.checkedRead;
import static tv.hd3g.datablock.NIOUtils.checkedWrite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO (after) add technical readme
// TODO (after) add v1 + object storage / serializers + defrag + indexed list
public class DatablockDocument {

	public static final int CHUNK_SEPARATOR_SIZE = 1;

	private final FileChannel channel;
	private final ByteBuffer documentHeaderBuffer = ByteBuffer.allocate(DOCUMENT_HEADER_LEN);
	private final ByteBuffer chunkHeaderBuffer = ByteBuffer.allocate(CHUNK_HEADER_LEN);
	private final ByteBuffer chunkSeparator = ByteBuffer.allocate(CHUNK_SEPARATOR_SIZE);

	public DatablockDocument(final FileChannel channel) throws IOException {
		this.channel = Objects.requireNonNull(channel, "\"channel\" can't to be null");
		if (channel.isOpen() == false) {
			throw new IOException("Can't works on closed channel");
		}
	}

	public synchronized DatablockDocumentHeader getDocumentHeader() throws IOException {
		documentHeaderBuffer.clear();
		checkedRead(channel, 0, documentHeaderBuffer);
		return new DatablockDocumentHeader(documentHeaderBuffer);
	}

	public synchronized void putDocumentHeader(final DatablockDocumentHeader header) throws IOException {
		final var buffer = header.toByteBuffer();
		checkedWrite(channel, 0, buffer);
	}

	private void goToEOF() throws IOException {
		channel.position(channel.size());
	}

	/**
	 * @param chunkPayload No reset/flip will be done
	 */
	public synchronized DataBlockChunkIndexItem appendChunk(final byte[] fourCC,
															final short version,
															final boolean archived,
															final ByteBuffer chunkPayload) throws IOException {
		goToEOF();
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, chunkPayload.remaining(), archived);

		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);
		final var payloadPosition = channel.position();
		checkedWrite(channel, chunkPayload);
		writeChunkSeparator();

		return new DataBlockChunkIndexItem(chunkHeader, payloadPosition);
	}

	public synchronized DataBlockChunkIndexItem appendChunk(final byte[] fourCC,
															final short version,
															final boolean archived,
															final OutputStreamConsumer writer) throws IOException {
		goToEOF();
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, 0, archived);
		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);
		final var payloadPosition = channel.position();

		try (var outputStream = new DatablockOutputStreamChunk(channel)) {
			writer.writeTo(outputStream);
		} finally {
			writeChunkSeparator();
		}

		final var payloadSize = (int) channel.position() - CHUNK_SEPARATOR_SIZE - (int) payloadPosition;

		return new DataBlockChunkIndexItem(
				new DatablockChunkHeader(fourCC, version, payloadSize, archived),
				payloadPosition);
	}

	public synchronized DataBlockChunkIndexItem appendEmptyChunk(final byte[] fourCC,
																 final short version,
																 final boolean archived,
																 final int payloadSize) throws IOException {
		goToEOF();
		final var chunkHeader = new DatablockChunkHeader(fourCC, version, payloadSize, archived);
		final var header = chunkHeader.toByteBuffer();
		checkedWrite(channel, header);

		final var payloadPosition = channel.position();
		channel.position(payloadPosition + payloadSize);
		writeChunkSeparator();

		return new DataBlockChunkIndexItem(chunkHeader, payloadPosition);
	}

	private void writeChunkSeparator() throws IOException {
		chunkSeparator.clear();
		chunkSeparator.put(ZERO_BYTE);
		chunkSeparator.flip();
		checkedWrite(channel, chunkSeparator);
	}

	public synchronized void documentCrawl(final FoundedDataBlockDocumentChunk chunkCallback) throws IOException {
		if (channel.size() < DOCUMENT_HEADER_LEN) {
			throw new IOException("Document has no header/too empty");
		}

		channel.position(DOCUMENT_HEADER_LEN);

		while (channel.position() < channel.size()) {
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

	/**
	 * @return new created document
	 */
	public synchronized DatablockDocument documentRefactor(final FileChannel newChannel,
														   final DatablockKeepChunkPolicy keepChunkPolicy) throws IOException {
		final var targetDocument = new DatablockDocument(newChannel);
		newChannel.truncate(DOCUMENT_HEADER_LEN);
		newChannel.position(0);

		final var actualHeader = getDocumentHeader();
		targetDocument.putDocumentHeader(actualHeader.getIncrementedDocumentVersion());

		documentCrawl((chunkHeader,
					   chunkPayloadDocumentPosition,
					   payloadExtractor) -> {
			final var policy = keepChunkPolicy.getPolicy(
					new DataBlockChunkIndexItem(chunkHeader, chunkPayloadDocumentPosition));

			if (policy.keepActualChunk()) {
				final var startChunkPos = chunkPayloadDocumentPosition - CHUNK_HEADER_LEN;
				final var chunkLen = CHUNK_HEADER_LEN
									 + chunkHeader.getPayloadSize()
									 + chunkSeparator.capacity();

				try {
					channel.transferTo(startChunkPos, chunkLen, newChannel);
				} catch (final IOException e) {
					throw new UncheckedIOException(
							"Can't transfert to newDocument (readed from=" + startChunkPos + ", len=" + chunkLen + ")",
							e);
				}

			}

			if (policy.changeActualChunk()) {
				try {
					final var impl = new DatablockChunkPayloadExtractorImpl(
							channel, chunkPayloadDocumentPosition, chunkHeader.getPayloadSize());

					impl.updateHeader(
							policy.markActualChunkAsArchived(),
							policy.markActualChunkAsDeleted());
				} catch (final IOException e) {
					throw new UncheckedIOException("Can't write to actual document", e);
				}
			}

		});

		return targetDocument;
	}

	DatablockChunkPayloadExtractorImpl createChunkPayloadExtractor(final long payloadPosition, final int payloadSize) {
		return new DatablockChunkPayloadExtractorImpl(channel, payloadPosition, payloadSize);
	}

}
