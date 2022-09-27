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

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.CachedFileAttributes;

class TransfertFilesSerializerTest {
	static Faker faker = Faker.instance();

	CachedFileAttributes cfa;
	ObjectMapper objectMapper;

	String path;
	long length;
	long lastModified;
	boolean exists;
	boolean directory;
	boolean file;
	boolean link;
	boolean special;

	@Mock
	AbstractFile abstractFile;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		objectMapper = new ObjectMapper();
		path = faker.artist().name();
		length = faker.random().nextLong();
		lastModified = faker.random().nextLong();
		exists = faker.random().nextBoolean();
		directory = faker.random().nextBoolean();
		file = faker.random().nextBoolean();
		link = faker.random().nextBoolean();
		special = faker.random().nextBoolean();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(abstractFile);
	}

	@Test
	void testCachedFileAttributeSerializer() {
		when(abstractFile.getPath()).thenReturn(path);
		cfa = new CachedFileAttributes(abstractFile, length, lastModified, exists, directory, file, link, special);

		final var result = objectMapper.valueToTree(cfa);

		final var fields = StreamSupport.stream(spliteratorUnknownSize(result.fieldNames(), ORDERED), false).toList();
		assertTrue(fields.contains("type"));
		assertTrue(fields.contains("path"));
		assertTrue(fields.contains("length"));
		assertTrue(fields.contains("lastModified"));
		assertTrue(fields.contains("exists"));
		assertTrue(fields.contains("directory"));
		assertTrue(fields.contains("file"));
		assertTrue(fields.contains("link"));
		assertTrue(fields.contains("special"));

		verify(abstractFile, times(1)).getPath();
	}

	@Test
	void testCachedFileAttributesDeserializer() throws JsonProcessingException, IllegalArgumentException {
		when(abstractFile.getPath()).thenReturn(path);
		final var source = new CachedFileAttributes(
				abstractFile, length, lastModified, exists, directory, file, link, special);
		final var json = objectMapper.valueToTree(source);
		verify(abstractFile, times(1)).getPath();

		final var value = objectMapper.treeToValue(json, CachedFileAttributes.class);
		assertEquals(path, value.getPath());
		assertEquals(length, value.length());
		assertEquals(lastModified, value.lastModified());
		assertEquals(exists, value.exists());
		assertEquals(directory, value.isDirectory());
		assertEquals(file, value.isFile());
		assertEquals(link, value.isLink());
		assertEquals(special, value.isSpecial());

		assertEquals(path, value.getAbstractFile().getPath());

		assertNull(value.getAbstractFile().getFileSystem());
		assertNull(value.getAbstractFile().getParent());
		assertNull(value.getAbstractFile().getName());
	}

}
