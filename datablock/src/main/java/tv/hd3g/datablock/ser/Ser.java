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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;

public class Ser {

	public Ser() throws IOException {
		// TODO create serializers
		// TODO check if all jackson deps are needed
		// TODO merge API

		final var jsonFactory = new JsonFactory();

		final var baos = new ByteArrayOutputStream();
		try (final var json = jsonFactory.createGenerator(baos, JsonEncoding.UTF8)) {
			json.writeStartObject();
			json.writeFieldName("report");
			json.writeStartObject();
			json.writeEndObject();
			json.writeEndObject();
		}

		var bais = new ByteArrayInputStream(baos.toByteArray());
		try (final var json = jsonFactory.createParser(bais)) {
			// https://jenkov.com/tutorials/java-json/jackson-jsonparser.html
		}

		baos.reset();
		final var xml = XMLEventFactory.newInstance();
		try {
			final var writer = XMLOutputFactory.newInstance()
					.createXMLEventWriter(baos);
			writer.add(xml.createStartDocument());
			writer.add(xml.createStartElement("", null, "REPORT"));
			writer.add(xml.createEndElement("", null, "REPORT"));
			writer.add(xml.createEndDocument());
			writer.close();
		} catch (final XMLStreamException e) {
			throw new IllegalStateException("Can't write XML file", e);
		}

		bais = new ByteArrayInputStream(baos.toByteArray());
		try {
			final var factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.newSAXParser().parse(new InputSource(bais), new SAXP());
		} catch (SAXException | ParserConfigurationException e) {
			throw new InternalError("Can't load SAX parser", e);
		}
	}

	class SAXP extends DefaultHandler {

		@Override
		public void startDocument() throws SAXException {
		}

		@Override
		public void endDocument() throws SAXException {
		}
	}

}
