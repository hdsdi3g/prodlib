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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

public class XMLEventWriterWrapper {// TODO test

	private final XMLEventWriter writer;
	private final XMLEventFactory xml;

	XMLEventWriterWrapper(final XMLEventWriter writer, final XMLEventFactory xml) {
		this.writer = requireNonNull(writer);
		this.xml = requireNonNull(xml);
	}

	private void add(final XMLEvent event) {
		try {
			writer.add(event);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException("Can't push an write event to XML stream", e);
		}
	}

	public void createAttribute(final String prefix,
								final String namespaceURI,
								final String localName,
								final String value) {
		add(xml.createAttribute(prefix, namespaceURI, localName, value));
	}

	public void createAttribute(final String localName, final String value) {
		add(xml.createAttribute(localName, value));
	}

	public void createAttribute(final QName name, final String value) {
		add(xml.createAttribute(name, value));
	}

	public void createNamespace(final String namespaceURI) {
		add(xml.createNamespace(namespaceURI));
	}

	public void createNamespace(final String prefix, final String namespaceUri) {
		add(xml.createNamespace(prefix, namespaceUri));
	}

	public void createStartElement(final QName name,
								   final Iterator<? extends Attribute> attributes,
								   final Iterator<? extends Namespace> namespaces) {
		add(xml.createStartElement(name, attributes, namespaces));
	}

	public void createStartElement(final String prefix, final String namespaceUri, final String localName) {
		add(xml.createStartElement(prefix, namespaceUri, localName));
	}

	public void createStartElement(final String prefix,
								   final String namespaceUri,
								   final String localName,
								   final Iterator<? extends Attribute> attributes,
								   final Iterator<? extends Namespace> namespaces) {
		add(xml.createStartElement(prefix, namespaceUri, localName, attributes, namespaces));
	}

	public void createStartElement(final String prefix,
								   final String namespaceUri,
								   final String localName,
								   final Iterator<? extends Attribute> attributes,
								   final Iterator<? extends Namespace> namespaces,
								   final NamespaceContext context) {
		add(xml.createStartElement(prefix, namespaceUri, localName, attributes, namespaces, context));
	}

	public void createEndElement(final QName name, final Iterator<? extends Namespace> namespaces) {
		add(xml.createEndElement(name, namespaces));
	}

	public void createEndElement(final String prefix, final String namespaceUri, final String localName) {
		add(xml.createEndElement(prefix, namespaceUri, localName));
	}

	public void createEndElement(final String prefix,
								 final String namespaceUri,
								 final String localName,
								 final Iterator<? extends Namespace> namespaces) {
		add(xml.createEndElement(prefix, namespaceUri, localName, namespaces));
	}

	public void createCharacters(final String content) {
		add(xml.createCharacters(content));
	}

	public void createCData(final String content) {
		add(xml.createCData(content));
	}

	public void createSpace(final String content) {
		add(xml.createSpace(content));
	}

	public void createIgnorableSpace(final String content) {
		add(xml.createIgnorableSpace(content));
	}

	public void createStartDocument() {
		add(xml.createStartDocument());
	}

	public void createStartDocument(final String encoding,
									final String version,
									final boolean standalone) {
		add(xml.createStartDocument(encoding, version, standalone));
	}

	public void createStartDocument(final String encoding, final String version) {
		add(xml.createStartDocument(encoding, version));
	}

	public void createStartDocument(final String encoding) {
		add(xml.createStartDocument(encoding));
	}

	public void createEndDocument() {
		add(xml.createEndDocument());
	}

	public void createEntityReference(final String name, final EntityDeclaration declaration) {
		add(xml.createEntityReference(name, declaration));
	}

	public void createComment(final String text) {
		add(xml.createComment(text));
	}

	public void createProcessingInstruction(final String target, final String data) {
		add(xml.createProcessingInstruction(target, data));
	}

	public void createDTD(final String dtd) {
		add(xml.createDTD(dtd));
	}

}
