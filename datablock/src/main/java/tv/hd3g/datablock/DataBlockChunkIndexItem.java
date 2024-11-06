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
import java.util.function.Function;

public record DataBlockChunkIndexItem(DatablockChunkHeader header, long payloadPosition) {

	public <T> T extractPayload(final Function<DatablockChunkPayloadExtractor, T> extractor,
								final DatablockDocument document) {
		final var reader = createChunkPayloadExtractor(document);
		T result;
		try {
			result = extractor.apply(reader);
		} finally {
			reader.clean();
		}

		return result;
	}

	public void setArchived(final boolean archived, final DatablockDocument document) throws IOException {
		createChunkPayloadExtractor(document).updateHeader(archived, header.isDeleted());
	}

	public void setDeleted(final boolean deleted, final DatablockDocument document) throws IOException {
		createChunkPayloadExtractor(document).updateHeader(header.isArchived(), deleted);
	}

	private DatablockChunkPayloadExtractorImpl createChunkPayloadExtractor(final DatablockDocument document) {
		return document.createChunkPayloadExtractor(payloadPosition, header.getPayloadSize());
	}

}
