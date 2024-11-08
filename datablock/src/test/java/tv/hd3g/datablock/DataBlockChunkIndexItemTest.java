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

import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DataBlockChunkIndexItemTest {

	@Mock
	DatablockChunkHeader header;
	@Mock
	DatablockDocument document;
	@Mock
	DatablockChunkPayloadExtractorImpl chunkPayloadExtractor;

	@Fake(min = 10, max = 10000)
	long position;
	@Fake(min = 10, max = 10000)
	int size;

	DataBlockChunkIndexItem i;

	@BeforeEach
	void init() {
		i = new DataBlockChunkIndexItem(header, position);
		when(header.getPayloadSize()).thenReturn(size);
		when(document.createChunkPayloadExtractor(position, size)).thenReturn(chunkPayloadExtractor);
	}

	@Test
	void testExtractPayload() {
		assertEquals(chunkPayloadExtractor, i.extractPayload(identity(), document));

		verify(header, atLeastOnce()).getPayloadSize();
		verify(document, times(1)).createChunkPayloadExtractor(position, size);
		verify(chunkPayloadExtractor, times(1)).clean();
	}

	@ParameterizedTest
	@MethodSource("tv.hd3g.commons.testtools.MockToolsExtendsJunit#provide2Booleans")
	void testSetArchived(final boolean archived, final boolean deleted) throws IOException {
		when(header.isDeleted()).thenReturn(deleted);

		i.setArchived(archived, document);

		verify(chunkPayloadExtractor, times(1)).updateHeader(archived, deleted);
		verify(header, atLeastOnce()).isDeleted();
		verify(header, atLeastOnce()).getPayloadSize();
		verify(document, times(1)).createChunkPayloadExtractor(position, size);
	}

	@ParameterizedTest
	@MethodSource("tv.hd3g.commons.testtools.MockToolsExtendsJunit#provide2Booleans")
	void testSetDeleted(final boolean archived, final boolean deleted) throws IOException {
		when(header.isArchived()).thenReturn(archived);

		i.setDeleted(deleted, document);

		verify(chunkPayloadExtractor, times(1)).updateHeader(archived, deleted);
		verify(header, atLeastOnce()).isArchived();
		verify(header, atLeastOnce()).getPayloadSize();
		verify(document, times(1)).createChunkPayloadExtractor(position, size);
	}

}
