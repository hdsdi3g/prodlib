/*
 * This file is part of transfertfiles.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.transfertfiles.json;

import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.CachedFileAttributes;

public class TransfertFilesSerializer {
	private static Logger log = LogManager.getLogger();

	private TransfertFilesSerializer() {
	}

	public static class CachedFileAttributeSerializer extends StdSerializer<CachedFileAttributes> {

		public CachedFileAttributeSerializer() {
			this(null);
		}

		public CachedFileAttributeSerializer(final Class<CachedFileAttributes> t) {
			super(t);
		}

		@Override
		public void serialize(final CachedFileAttributes value,
							  final JsonGenerator jg,
							  final SerializerProvider provider) throws IOException {
			jg.writeStartObject();

			jg.writeStringField("type", value.getAbstractFile().getClass().getSimpleName());
			jg.writeStringField("path", value.getPath());
			jg.writeNumberField("length", value.length());
			jg.writeStringField("lastModified", ISO_INSTANT.format(ofEpochMilli(value.lastModified())));
			jg.writeBooleanField("exists", value.exists());
			jg.writeBooleanField("directory", value.isDirectory());
			jg.writeBooleanField("file", value.isFile());
			jg.writeBooleanField("link", value.isLink());
			jg.writeBooleanField("special", value.isSpecial());

			jg.writeEndObject();
		}
	}

	public static class CachedFileAttributesDeserializer extends StdDeserializer<CachedFileAttributes> {

		public CachedFileAttributesDeserializer() {
			this(null);
		}

		public CachedFileAttributesDeserializer(final Class<?> t) {
			super(t);
		}

		@Override
		public CachedFileAttributes deserialize(final JsonParser jp,
												final DeserializationContext ctxt) throws IOException {
			final var node = jp.getCodec().readTree(jp);

			final var path = ((TextNode) node.get("path")).asText();
			final var length = ((LongNode) node.get("length")).asLong();
			final var lastModifiedText = ((TextNode) node.get("lastModified")).asText();
			final var lastModified = Instant.from(ISO_INSTANT.parse(lastModifiedText)).toEpochMilli();
			final var exists = ((BooleanNode) node.get("exists")).asBoolean();
			final var directory = ((BooleanNode) node.get("directory")).asBoolean();
			final var file = ((BooleanNode) node.get("file")).asBoolean();
			final var link = ((BooleanNode) node.get("link")).asBoolean();
			final var special = ((BooleanNode) node.get("special")).asBoolean();

			final var abstractFile = (AbstractFile) Proxy.newProxyInstance(
					TransfertFilesSerializer.class.getClassLoader(),
					new Class[] { AbstractFile.class },
					(proxy, method, args) -> {
						if (method.getName().equals("getPath")) {
							return path;
						}
						log.warn("This instance is a Proxy extracted from a Json, you can't access directly to it. {}",
								node);
						return null;
					});

			return new CachedFileAttributes(abstractFile, length, lastModified, exists, directory, file, link, special);
		}
	}

}
