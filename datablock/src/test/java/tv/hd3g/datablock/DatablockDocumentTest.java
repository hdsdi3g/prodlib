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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.datablock.DatablockChunkHeader.CHUNK_HEADER_LEN;
import static tv.hd3g.datablock.DatablockChunkHeader.FOURCC_EXPECTED_SIZE;
import static tv.hd3g.datablock.DatablockDocument.CHUNK_SEPARATOR_SIZE;
import static tv.hd3g.datablock.DatablockDocumentHeader.DOCUMENT_HEADER_LEN;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunk;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkThenMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.dontKeepChunkThenMarkActualDeleted;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunk;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkThenMarkActualArchived;
import static tv.hd3g.datablock.DatablockMigrateChunkPolicy.keepChunkThenMarkActualDeleted;
import static tv.hd3g.datablock.NIOUtils.ZERO_BYTE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DatablockDocumentTest extends RealFileWork {

	private static final int DOCUMENT_POS0 = DOCUMENT_HEADER_LEN
											 + CHUNK_HEADER_LEN;

	@Fake(min = 100, max = 1000)
	long invalidStartPosition;

	@Fake(min = 8, max = 8)
	byte[] magicNumber;
	@Fake(min = 8, max = 8)
	byte[] documentType;
	@Fake
	short typeVersion;
	@Fake
	int documentVersion;

	@Fake(min = FOURCC_EXPECTED_SIZE, max = FOURCC_EXPECTED_SIZE)
	byte[] fourCC;
	@Fake
	short version;
	@Fake
	boolean archived;

	@Fake(min = FOURCC_EXPECTED_SIZE, max = FOURCC_EXPECTED_SIZE)
	byte[] fourCC1;
	@Fake
	short version1;
	@Fake
	boolean archived1;
	@Fake(min = 1000, max = 10000)
	byte[] data1;

	@Mock
	FoundedDataBlockDocumentChunk foundedDataBlockDocumentChunk;

	DatablockDocumentHeader ddh;
	int documentPos1;

	DatablockDocument d;

	@BeforeEach
	void init() throws Exception {
		channel = FileChannel.open(file.toPath(), CREATE, READ, WRITE);
		channel.truncate(0);
		d = new DatablockDocument(channel);
		ddh = new DatablockDocumentHeader(magicNumber, documentType, typeVersion, documentVersion);

		documentPos1 = DOCUMENT_POS0
					   + data.length
					   + CHUNK_SEPARATOR_SIZE
					   + CHUNK_HEADER_LEN;
	}

	@Test
	void testDatablockDocument_closed() throws IOException {
		channel.close();
		assertThrows(IOException.class, () -> new DatablockDocument(channel));
	}

	@Test
	void testGetDocumentHeader_emptyFile() {
		assertThrows(IOException.class, () -> d.getDocumentHeader());
	}

	@Test
	void testPutDocumentHeader() throws IOException {
		assertEquals(0, file.length());
		assertEquals(0, channel.position());

		channel.position(invalidStartPosition);
		d.putDocumentHeader(ddh);

		assertEquals(DOCUMENT_HEADER_LEN, file.length());
		assertEquals(DOCUMENT_HEADER_LEN, channel.position());
	}

	@Test
	void testGetDocumentHeader() throws IOException {
		d.putDocumentHeader(ddh);
		d = new DatablockDocument(channel);
		channel.position(invalidStartPosition);
		assertEquals(ddh, d.getDocumentHeader());
		assertEquals(DOCUMENT_HEADER_LEN, file.length());
		assertEquals(DOCUMENT_HEADER_LEN, channel.position());
	}

	@Test
	void testAppendChunk_byteBuffer() throws IOException {
		channel.position(invalidStartPosition);
		final var item = d.appendChunk(fourCC, version, archived, ByteBuffer.wrap(data));
		checksAppendChunk(item);
		checkWritedChunk();
	}

	@Test
	void testAppendChunk_outputStream() throws IOException {
		channel.position(invalidStartPosition);
		final var item = d.appendChunk(fourCC, version, archived, writer -> writer.write(data));
		checksAppendChunk(item);
		checkWritedChunk();
	}

	@Test
	void testAppendEmptyChunk() throws IOException {
		channel.position(invalidStartPosition);
		Arrays.fill(data, ZERO_BYTE);
		final var item = d.appendEmptyChunk(fourCC, version, archived, data.length);
		checksAppendChunk(item);
		checkWritedChunk();
	}

	@Test
	void testDocumentCrawl_emptyFile() {
		assertThrows(IOException.class, () -> d.documentCrawl(foundedDataBlockDocumentChunk));
	}

	@Test
	void testDocumentCrawl_noChunks() throws IOException {
		d.putDocumentHeader(ddh);
		channel.position(invalidStartPosition);
		assertDoesNotThrow(() -> d.documentCrawl(foundedDataBlockDocumentChunk));
	}

	@Test
	void testDocumentCrawl_chunks() throws IOException {
		d.putDocumentHeader(ddh);
		d.appendChunk(fourCC, version, archived, ByteBuffer.wrap(data));
		d.appendChunk(fourCC1, version1, archived1, ByteBuffer.wrap(data1));

		channel.position(invalidStartPosition);

		final var calledThreadId = new AtomicLong(-1);

		d.documentCrawl((h, chunkPayloadDocumentPosition, payloadExtractor) -> {
			calledThreadId.set(Thread.currentThread().threadId());

			if (Arrays.equals(fourCC, h.getFourCC())) {
				assertEquals(DOCUMENT_POS0,
						chunkPayloadDocumentPosition);
				checkChunkContent(fourCC, version, archived, data, h, payloadExtractor);
			} else if (Arrays.equals(fourCC1, h.getFourCC())) {
				assertEquals(documentPos1,
						chunkPayloadDocumentPosition);
				checkChunkContent(fourCC1, version1, archived1, data1, h, payloadExtractor);
			}
		});

		assertEquals(Thread.currentThread().threadId(), calledThreadId.get());
		assertEquals(file.length(), channel.position());
	}

	@Test
	void testDocumentCrawl_UpdateHeader() throws IOException {
		d.putDocumentHeader(ddh);
		d.appendChunk(fourCC, version, false, ByteBuffer.wrap(data));
		d.appendChunk(fourCC1, version1, true, ByteBuffer.wrap(data1));

		channel.position(invalidStartPosition);

		d.documentCrawl((h, chunkPayloadDocumentPosition, payloadExtractor) -> {
			if (Arrays.equals(fourCC, h.getFourCC())) {
				payloadExtractor.updateHeader(true, false);
			} else if (Arrays.equals(fourCC1, h.getFourCC())) {
				payloadExtractor.updateHeader(false, true);
			}
		});

		d.documentCrawl((h, chunkPayloadDocumentPosition, payloadExtractor) -> {
			if (Arrays.equals(fourCC, h.getFourCC())) {
				assertTrue(h.isArchived());
				assertFalse(h.isDeleted());
				assertArrayEquals(data, payloadExtractor.getCurrentChunkPayloadBytes());
			} else if (Arrays.equals(fourCC1, h.getFourCC())) {
				assertFalse(h.isArchived());
				assertTrue(h.isDeleted());
				assertArrayEquals(data1, payloadExtractor.getCurrentChunkPayloadBytes());
			}
		});

		final var map = d.getDocumentMap();
		assertThat(map).hasSize(2);
		var h = map.get(0).header();
		assertTrue(h.isArchived());
		assertFalse(h.isDeleted());
		h = map.get(1).header();
		assertFalse(h.isArchived());
		assertTrue(h.isDeleted());
	}

	@Test
	void testGetDocumentMap() throws IOException {
		d.putDocumentHeader(ddh);
		d.appendChunk(fourCC, version, archived, ByteBuffer.wrap(data));
		d.appendChunk(fourCC1, version1, archived1, ByteBuffer.wrap(data1));

		channel.position(invalidStartPosition);

		final var map = d.getDocumentMap();
		assertThat(map).hasSize(2);
		/**
		 * Created only to check get+append+get and DataBlockChunkIndexItem solidity
		 */
		d.appendChunk(fourCC, version, false, ByteBuffer.wrap(data));

		var item0 = map.get(0);
		final var payloadExtractor0 = item0.extractPayload(extractor -> {
			checkChunkContent(fourCC,
					version,
					archived,
					data,
					map.get(0).header(),
					extractor);
			return extractor;
		}, d);

		d.appendChunk(fourCC, version, false, ByteBuffer.wrap(data));

		var item1 = map.get(1);
		final var payloadExtractor1 = item1.extractPayload(extractor -> {
			checkChunkContent(fourCC1,
					version1,
					archived1,
					data1,
					map.get(1).header(),
					extractor);
			return extractor;
		}, d);

		assertThat(map).hasSize(2);

		assertNotNull(payloadExtractor0);
		assertNotNull(payloadExtractor1);
		assertArrayEquals(data, payloadExtractor0.getCurrentChunkPayloadBytes());
		assertArrayEquals(data1, payloadExtractor1.getCurrentChunkPayloadBytes());

		assertEquals(DOCUMENT_POS0, item0.payloadPosition());
		assertEquals(documentPos1, item1.payloadPosition());

		item0.setArchived(archived == false, d);
		item0 = documentMapUpdateCheck(0, archived == false, false);
		item0.setDeleted(true, d);
		item0 = documentMapUpdateCheck(0, archived == false, true);
		item0.setArchived(archived, d);
		item0 = documentMapUpdateCheck(0, archived, true);
		item0.setDeleted(false, d);
		documentMapUpdateCheck(0, archived, false);

		item1.setArchived(archived1 == false, d);
		item1 = documentMapUpdateCheck(1, archived1 == false, false);
		item1.setDeleted(true, d);
		item1 = documentMapUpdateCheck(1, archived1 == false, true);
		item1.setArchived(archived1, d);
		item1 = documentMapUpdateCheck(1, archived1, true);
		item1.setDeleted(false, d);
		documentMapUpdateCheck(1, archived1, false);

		assertEquals(file.length(), channel.position());
		assertEquals(DOCUMENT_HEADER_LEN
					 + CHUNK_HEADER_LEN * 4
					 + data.length * 3
					 + data1.length
					 + CHUNK_SEPARATOR_SIZE * 4, file.length());
	}

	@Nested
	class DocumentRefactor {

		DatablockDocumentHeader nextHeader;
		DatablockDocument newDocument;
		List<DataBlockChunkIndexItem> newChuncks;
		List<DataBlockChunkIndexItem> initialChuncks;
		List<DataBlockChunkIndexItem> updatedChuncks;
		List<byte[]> newChunckDatas;

		@BeforeEach
		void init() throws Exception {
			d.putDocumentHeader(ddh);
			initialChuncks = List.of(
					d.appendChunk(fourCC, version, false, ByteBuffer.wrap(data)),
					d.appendChunk(fourCC1, version1, true, ByteBuffer.wrap(data1)));
			newChunckDatas = List.of(data, data1);

			prepareFile1();

			channel1 = FileChannel.open(file1.toPath(), CREATE, READ, WRITE);
			channel1.truncate(0);

			nextHeader = new DatablockDocumentHeader(magicNumber, documentType, typeVersion, documentVersion + 1);

			channel.position(invalidStartPosition);
		}

		@Test
		void testAllItems() throws IOException {
			final var itemList = new ArrayList<DataBlockChunkIndexItem>();
			newDocument = d.documentRefactor(channel1, item -> {
				itemList.add(item);
				return keepChunk();
			});
			assertEquals(d.getDocumentMap(), itemList);
		}

		@Nested
		class Keep {

			private void doOperation(final DatablockMigrateChunkPolicy policy) throws IOException {
				newDocument = d.documentRefactor(channel1, item -> policy);
				assertNotNull(newDocument);
				assertEquals(nextHeader, newDocument.getDocumentHeader());
				assertEquals(file.length(), file1.length());

				newChuncks = newDocument.getDocumentMap();
				updatedChuncks = d.getDocumentMap();
			}

			@Test
			void testKeepChunk() throws IOException {
				doOperation(keepChunk());

				for (var pos = 0; pos < initialChuncks.size(); pos++) {
					final var initialArchived = initialChuncks.get(pos).header().isArchived();
					checkChunk(
							initialChuncks.get(pos),
							initialArchived,
							false,
							updatedChuncks.get(pos));

					checkChunk(
							initialChuncks.get(pos),
							initialArchived,
							false,
							newChuncks.get(pos));

					assertArrayEquals(newChunckDatas.get(pos),
							newChuncks.get(pos).extractPayload(newDocument));
				}
			}

			@Test
			void testKeepChunkThenMarkActualDeleted() throws IOException {
				checkKeepChunkThenMarkActualCheck(keepChunkThenMarkActualDeleted());
			}

			@Test
			void testKeepChunkThenMarkActualArchived() throws IOException {
				checkKeepChunkThenMarkActualCheck(keepChunkThenMarkActualArchived());
			}

			private void checkKeepChunkThenMarkActualCheck(final DatablockMigrateChunkPolicy policy) throws IOException {
				doOperation(policy);

				for (var pos = 0; pos < initialChuncks.size(); pos++) {
					final var initialArchived = initialChuncks.get(pos).header().isArchived();
					checkChunk(
							initialChuncks.get(pos),
							policy.markActualChunkAsArchived(),
							policy.markActualChunkAsDeleted(),
							updatedChuncks.get(pos));

					checkChunk(
							initialChuncks.get(pos),
							initialArchived,
							false,
							newChuncks.get(pos));

					assertArrayEquals(newChunckDatas.get(pos),
							newChuncks.get(pos).extractPayload(newDocument));
				}
			}
		}

		@Nested
		class DontKeep {

			private void prepareOperation(final DatablockMigrateChunkPolicy policy) throws IOException {
				newDocument = d.documentRefactor(channel1, item -> policy);
				assertNotNull(newDocument);
				assertEquals(nextHeader, newDocument.getDocumentHeader());
				assertEquals(DOCUMENT_HEADER_LEN, file1.length());
				assertEquals(DOCUMENT_HEADER_LEN, channel1.position());
				assertThat(newDocument.getDocumentMap()).isEmpty();
			}

			@Test
			void testDontKeepChunk() throws IOException {
				prepareOperation(dontKeepChunk());
			}

			@Test
			void testDontKeepChunkThenMarkActualDeleted() throws IOException {
				final var policy = dontKeepChunkThenMarkActualDeleted();
				prepareOperation(policy);
				checkUpdates(policy);
			}

			@Test
			void testDontKeepChunkThenMarkActualArchived() throws IOException {
				final var policy = dontKeepChunkThenMarkActualArchived();
				prepareOperation(policy);
				checkUpdates(policy);
			}

			private void checkUpdates(final DatablockMigrateChunkPolicy policy) throws IOException {
				updatedChuncks = d.getDocumentMap();
				for (var pos = 0; pos < initialChuncks.size(); pos++) {
					checkChunk(
							initialChuncks.get(pos),
							policy.markActualChunkAsArchived(),
							policy.markActualChunkAsDeleted(),
							updatedChuncks.get(pos));
				}
			}

		}

		private void checkChunk(final DataBlockChunkIndexItem actualChunck,
								final boolean archived,
								final boolean deleted,
								final DataBlockChunkIndexItem item) {
			final var actualChunckHeader = actualChunck.header();
			final var h = item.header();

			assertThat(h.getFourCC()).containsExactly(actualChunckHeader.getFourCC());
			assertEquals(actualChunckHeader.getVersion(), h.getVersion());
			assertEquals(actualChunckHeader.getCreatedDate(), h.getCreatedDate());
			assertEquals(actualChunckHeader.getPayloadSize(), h.getPayloadSize());

			assertEquals(archived, h.isArchived(), "Invalid archived flag");
			assertEquals(deleted, h.isDeleted(), "Invalid deleted flag");
		}

	}

	private void checksAppendChunk(final DataBlockChunkIndexItem item) throws IOException {
		final var h = item.header();
		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(data.length, h.getPayloadSize());
		assertEquals(archived, h.isArchived());
		assertFalse(h.isDeleted());
		final var now = System.currentTimeMillis();
		assertThat(h.getCreatedDate()).isBetween(now - 1000, now);
		assertEquals(DOCUMENT_POS0 - DOCUMENT_HEADER_LEN, item.payloadPosition());
		assertArrayEquals(data, item.extractPayload(d));
	}

	private DataBlockChunkIndexItem documentMapUpdateCheck(final int index,
														   final boolean archived,
														   final boolean deleted) throws IOException {
		final var item = d.getDocumentMap().get(index);
		final var header = item.header();
		assertEquals(archived, header.isArchived());
		assertEquals(deleted, header.isDeleted());
		return item;
	}

	private void checkWritedChunk() throws IOException {
		assertEquals(CHUNK_HEADER_LEN + data.length + CHUNK_SEPARATOR_SIZE, file.length());
		assertEquals(file.length(), channel.position());

		channel.position(0);
		final var realData = readFileToByteArray(file);

		assertTrue(Arrays.equals(
				data, 0, data.length,
				realData, CHUNK_HEADER_LEN, CHUNK_HEADER_LEN + data.length));

		final var chunkHeaderByteBuffer = ByteBuffer.wrap(realData, 0, CHUNK_HEADER_LEN);
		final var h = new DatablockChunkHeader(chunkHeaderByteBuffer);

		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(data.length, h.getPayloadSize());
		assertEquals(archived, h.isArchived());
		assertFalse(h.isDeleted());
		final var now = System.currentTimeMillis();
		assertThat(h.getCreatedDate()).isBetween(now - 1000, now);

		final var zeros = new byte[CHUNK_SEPARATOR_SIZE];
		assertTrue(Arrays.equals(
				zeros, 0, zeros.length,
				realData, CHUNK_HEADER_LEN + data.length, (int) file.length()));

	}

	private void checkChunkContent(final byte[] fourCC,
								   final short version,
								   final boolean archived,
								   final byte[] data,
								   final DatablockChunkHeader h,
								   final DatablockChunkPayloadExtractor payloadExtractor) throws IOException {
		assertThat(h.getFourCC()).containsExactly(fourCC);
		assertEquals(version, h.getVersion());
		assertEquals(data.length, h.getPayloadSize());
		assertEquals(archived, h.isArchived());
		assertFalse(h.isDeleted());

		final var now = System.currentTimeMillis();
		assertThat(h.getCreatedDate()).isBetween(now - 1000, now);

		assertArrayEquals(data, payloadExtractor.getCurrentChunkPayloadBytes());

		final var mBB = payloadExtractor.getCurrentChunkPayload();
		final var dataReadBB = new byte[mBB.remaining()];
		mBB.get(dataReadBB);
		assertArrayEquals(data, dataReadBB);

		final var chunkReader = new ByteArrayOutputStream(data.length);
		assertEquals(data.length,
				payloadExtractor.createInputStream(inputStream -> inputStream.transferTo(chunkReader)));
		assertArrayEquals(data, chunkReader.toByteArray());
	}

}
