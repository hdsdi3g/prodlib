/*
 * This file is part of jobkit-engine.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.jobkit.watchfolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.CachedFileAttributes;

class WatchedFilesTest {

	@Mock
	Set<CachedFileAttributes> founded;
	@Mock
	Set<CachedFileAttributes> losted;
	@Mock
	Set<CachedFileAttributes> updated;
	int totalFiles;

	@Mock
	CachedFileAttributes cfa0;
	@Mock
	CachedFileAttributes cfa1;
	@Mock
	CachedFileAttributes cfa2;
	@Mock
	AbstractFile abstractFile;
	@Mock
	AbstractFileSystem<AbstractFile> abstractFileSystem;

	WatchedFiles watchedFiles;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		totalFiles = new Random().nextInt();
		watchedFiles = new WatchedFiles(founded, losted, updated, totalFiles);
	}

	@Test
	void testToString() {
		assertNotNull(watchedFiles.toString());

		verify(founded, times(1)).stream();
		verify(losted, times(1)).stream();
		verify(updated, times(1)).stream();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(founded, losted, updated, abstractFile, abstractFileSystem);
	}

	@Test
	void testFoundedAndUpdated() {
		when(founded.stream()).thenReturn(Stream.of(cfa0, cfa1));
		when(updated.stream()).thenReturn(Stream.of(cfa1, cfa2));

		final var result = watchedFiles.foundedAndUpdated();
		assertEquals(3, result.size());
		assertTrue(result.contains(cfa0));
		assertTrue(result.contains(cfa1));
		assertTrue(result.contains(cfa2));

		verify(founded, times(1)).stream();
		verify(updated, times(1)).stream();
	}

	@Test
	void testGetFoundedAndUpdatedFS() {
		when(founded.stream()).thenReturn(Stream.of(cfa0));
		when(updated.stream()).thenReturn(Stream.of());
		when(cfa0.getAbstractFile()).thenReturn(abstractFile);
		when((AbstractFileSystem<AbstractFile>) abstractFile.getFileSystem()).thenReturn(abstractFileSystem);

		final var result = watchedFiles.getFoundedAndUpdatedFS();
		assertEquals(1, result.size());
		assertTrue(result.contains(abstractFileSystem));

		verify(founded, times(1)).stream();
		verify(updated, times(1)).stream();
		verify(cfa0, times(1)).getAbstractFile();
		verify(abstractFile, times(1)).getFileSystem();
	}

}
