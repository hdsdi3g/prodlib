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
package tv.hd3g.datablock.ser;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.datablock.DataBlockChunkIndexItem;
import tv.hd3g.datablock.DatablockChunkPayloadExtractor;
import tv.hd3g.datablock.DatablockDocument;

/**
 * Stateless, reusable
 */
@Slf4j
public class DatablockObjectSerializer {// TODO test

	private final SAXParserFactory saxParserFactory;
	private final XMLEventFactory xmlEventFactory;
	private final XMLOutputFactory xmlOutputFactory;
	private final JsonFactory jsonFactory;

	public DatablockObjectSerializer() {
		saxParserFactory = SAXParserFactory.newInstance();
		try {
			saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException e) {
			throw new InternalError("Can't load SAX parser", e);
		}

		xmlEventFactory = XMLEventFactory.newInstance();
		xmlOutputFactory = XMLOutputFactory.newInstance();
		jsonFactory = new JsonFactory();
	}

	public <T, H extends DefaultHandler> T xmlDeserialize(final DatablockChunkPayloadExtractor payloadSource,
														  final H handler,
														  final Function<H, T> processor) throws IOException {
		return payloadSource.createInputStream(inputStream -> {
			try {
				saxParserFactory.newSAXParser().parse(new InputSource(inputStream), handler);
			} catch (ParserConfigurationException | SAXException e) {
				throw new ImportExportException("Can't process XML", e);
			}
			return processor.apply(handler);
		});
	}

	public <T> DataBlockChunkIndexItem xmlSerialize(final DatablockDocument document,
													final byte[] fourCC,
													final short version,
													final boolean archived,
													final T object,
													final BiConsumer<T, XMLEventWriterWrapper> serializer) throws IOException {

		return document.appendChunk(fourCC, version, archived,
				outputStream -> {
					try {
						final var writer = xmlOutputFactory.createXMLEventWriter(outputStream);
						final var xmlEventWriterWrapper = new XMLEventWriterWrapper(writer, xmlEventFactory);
						serializer.accept(object, xmlEventWriterWrapper);
						writer.close();
					} catch (final XMLStreamException e) {
						throw new ImportExportException("Can't make XML document", e);
					}
				});
	}

	public <T, H extends JsonParserEventHandler> T jsonDeserialize(final DatablockChunkPayloadExtractor payloadSource,
																   final H handler,
																   final Function<H, T> processor) throws IOException {
		return payloadSource.createInputStream(inputStream -> {
			try (final var parser = jsonFactory.createParser(inputStream)) {
				jsonCrawnReader(handler, parser);
			}
			return processor.apply(handler);
		});
	}

	void jsonCrawnReader(final JsonParserEventHandler handler, final JsonParser parser) throws IOException { // NOSONAR 3776
		String fieldName = null;
		while (!parser.isClosed()) {
			final var jsonToken = parser.nextToken();

			if (jsonToken == JsonToken.START_OBJECT) {
				handler.startObject();
				fieldName = null;
			} else if (jsonToken == JsonToken.END_OBJECT) {
				handler.endObject();
				fieldName = null;
			} else if (jsonToken == JsonToken.START_ARRAY) {
				handler.startArray();
				fieldName = null;
			} else if (jsonToken == JsonToken.END_ARRAY) {
				handler.endArray();
				fieldName = null;
			} else if (jsonToken == JsonToken.FIELD_NAME) {
				if (fieldName != null) {
					log.warn("Json fieldName on fieldName ? {} on {} ({}) ?",
							fieldName, parser.currentName(), parser);
				}
				fieldName = parser.currentName();
			} else if (jsonToken == JsonToken.VALUE_STRING) {
				if (fieldName != null) {
					handler.field(fieldName, parser.getValueAsString());
				} else {
					handler.value(parser.getValueAsString());
				}
				fieldName = null;
			} else if (jsonToken == JsonToken.VALUE_NUMBER_INT) {
				if (fieldName != null) {
					handler.field(fieldName, parser.getValueAsLong());
				} else {
					handler.value(parser.getValueAsLong());
				}
				fieldName = null;
			} else if (jsonToken == JsonToken.VALUE_NUMBER_FLOAT) {
				if (fieldName != null) {
					handler.field(fieldName, parser.getValueAsDouble());
				} else {
					handler.value(parser.getValueAsDouble());
				}
				fieldName = null;
			} else if (jsonToken == JsonToken.VALUE_TRUE || jsonToken == JsonToken.VALUE_FALSE) {
				if (fieldName != null) {
					handler.field(fieldName, jsonToken == JsonToken.VALUE_TRUE);
				} else {
					handler.value(jsonToken == JsonToken.VALUE_TRUE);
				}
				fieldName = null;
			} else if (jsonToken == JsonToken.VALUE_NULL) {
				if (fieldName != null) {
					handler.nullField(fieldName);
				} else {
					handler.nullValue();
				}
				fieldName = null;
			} else {
				log.warn("Non managed Json: {} on {}", jsonToken, parser);
			}
		}
	}

	public <T> DataBlockChunkIndexItem jsonSerialize(final DatablockDocument document,
													 final byte[] fourCC,
													 final short version,
													 final boolean archived,
													 final T object,
													 final JsonGeneratorConsumer<T> serializer) throws IOException {
		return document.appendChunk(fourCC, version, archived,
				outputStream -> {
					try (final var jsonGenerator = jsonFactory.createGenerator(outputStream, UTF8)) {
						serializer.accept(object, jsonGenerator);
					}
				});
	}

}
