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

import java.nio.ByteBuffer;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class DatablockDocumentHeader implements IOTraits {// TODO debug tools

	public static final int MAGIC_NUMBER_EXPECTED_SIZE = 8;
	public static final int DOCUMENT_TYPE_EXPECTED_SIZE = 8;
	public static final int BLANK_EXPECTED_SIZE = 2;
	public static final int DOCUMENT_HEADER_LEN = MAGIC_NUMBER_EXPECTED_SIZE +
												  DOCUMENT_TYPE_EXPECTED_SIZE
												  + 2 /** typeVersion */
												  + 4 /** documentVersion */
												  + BLANK_EXPECTED_SIZE;

	private final byte[] magicNumber;
	private final byte[] documentType;
	private final short typeVersion;
	private final int documentVersion;

	/**
	 * @param magicNumber to identify the file with "magic" tools. Should tie in with file extension.
	 * @param documentType to identify the kind of managed document.
	 * @param typeVersion to identify the parser version to manage this document.
	 * @param documentVersion to keep a trace if the document change (overwrite) with a new version.
	 */
	public DatablockDocumentHeader(final byte[] magicNumber,
								   final byte[] documentType,
								   final short typeVersion,
								   final int documentVersion) {
		if (magicNumber.length != MAGIC_NUMBER_EXPECTED_SIZE) {
			throw new IllegalArgumentException("magicNumber len must equals "
											   + MAGIC_NUMBER_EXPECTED_SIZE + BYTES_STR);
		}
		if (documentType.length != DOCUMENT_TYPE_EXPECTED_SIZE) {
			throw new IllegalArgumentException("documentType len must equals "
											   + DOCUMENT_TYPE_EXPECTED_SIZE + BYTES_STR);
		}
		this.magicNumber = magicNumber;
		this.documentType = documentType;
		this.typeVersion = typeVersion;
		this.documentVersion = documentVersion;
	}

	DatablockDocumentHeader getIncrementedDocumentVersion() {
		return new DatablockDocumentHeader(magicNumber, documentType, typeVersion, documentVersion + 1);
	}

	DatablockDocumentHeader(final ByteBuffer readFrom) {
		checkRemaining(readFrom, DOCUMENT_HEADER_LEN);
		magicNumber = new byte[MAGIC_NUMBER_EXPECTED_SIZE];
		readFrom.get(magicNumber);
		documentType = new byte[DOCUMENT_TYPE_EXPECTED_SIZE];
		readFrom.get(documentType);
		typeVersion = readFrom.getShort();
		documentVersion = readFrom.getInt();

		checkEndBlank(readFrom, BLANK_EXPECTED_SIZE);
	}

	ByteBuffer toByteBuffer() {
		final var header = ByteBuffer.allocate(DOCUMENT_HEADER_LEN);
		header.put(magicNumber);
		header.put(documentType);
		header.putShort(typeVersion);
		header.putInt(documentVersion);
		header.put(new byte[BLANK_EXPECTED_SIZE]);
		header.flip();
		return header;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("Header [magicNumber=");
		builder.append(encodeHexString(magicNumber));
		builder.append(", documentType=");
		builder.append(encodeHexString(documentType));
		builder.append(", typeVersion=");
		builder.append(typeVersion);
		builder.append(", documentVersion=");
		builder.append(documentVersion);
		builder.append("]");
		return builder.toString();
	}

}
