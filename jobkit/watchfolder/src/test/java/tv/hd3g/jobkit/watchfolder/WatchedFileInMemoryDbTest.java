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

import static java.time.Duration.ZERO;
import static java.time.Duration.ofDays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.DIRS_ONLY;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_DIRS;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_ONLY;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tv.hd3g.transfertfiles.CachedFileAttributes;

class WatchedFileInMemoryDbTest {

	@Mock
	CachedFileAttributes firstDetectionFile;
	@Mock
	CachedFileAttributes seeAgainFile;

	WatchedFileInMemoryDb f;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		when(firstDetectionFile.isDirectory()).thenReturn(false);
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, ofDays(1));
		verify(firstDetectionFile, times(1)).isDirectory();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(firstDetectionFile, seeAgainFile);
	}

	@Test
	void testUpdate_updated_inTime() {
		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(2L);

		assertEquals(f, f.update(seeAgainFile));

		verify(firstDetectionFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).lastModified();

		assertFalse(f.isTimeQualified());
		assertFalse(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());
	}

	@Test
	void testUpdate_notUpdated_inTime() {
		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(1L);
		when(firstDetectionFile.length()).thenReturn(1L);
		when(seeAgainFile.length()).thenReturn(1L);

		assertEquals(f, f.update(seeAgainFile));

		verify(firstDetectionFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).lastModified();
		verify(firstDetectionFile, times(1)).length();
		verify(seeAgainFile, times(1)).length();

		assertFalse(f.isTimeQualified());
		assertFalse(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());
	}

	@Test
	void testUpdate_updated_OutTime() {
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, ZERO);
		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(2L);

		assertEquals(f, f.update(seeAgainFile));

		verify(firstDetectionFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).lastModified();

		assertFalse(f.isTimeQualified());
		assertFalse(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
	}

	@Test
	void testUpdate_notUpdated_OutTime() throws InterruptedException {
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, ZERO);

		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(1L);
		when(firstDetectionFile.length()).thenReturn(1L);
		when(seeAgainFile.length()).thenReturn(1L);

		assertEquals(f, f.update(seeAgainFile));

		verify(firstDetectionFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).lastModified();
		verify(firstDetectionFile, times(1)).length();
		verify(seeAgainFile, times(1)).length();

		Thread.sleep(5);// NOSONAR 2925
		assertTrue(f.isTimeQualified());
		assertTrue(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
	}

	@Test
	void testUpdate_afterDone() {
		f.setMarkedAsDone();
		assertEquals(f, f.update(seeAgainFile));
		assertFalse(f.isTimeQualified());
		assertFalse(f.canBeCallbacked());
		assertFalse(f.isNotYetMarkedAsDone());
		/**
		 * Done, but changed
		 */
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());

		verify(firstDetectionFile, times(1)).lastModified();
		verify(firstDetectionFile, times(1)).length();
		verify(seeAgainFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).length();
	}

	@Test
	void testChangeFile_afterDone() throws InterruptedException {
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, ZERO);
		f.setMarkedAsDone();

		when(firstDetectionFile.length()).thenReturn(1L);
		when(seeAgainFile.length()).thenReturn(2L);

		assertEquals(f, f.update(seeAgainFile));

		when(firstDetectionFile.length()).thenReturn(2L);
		when(seeAgainFile.length()).thenReturn(2L);
		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(1L);

		assertEquals(f, f.update(seeAgainFile));

		Thread.sleep(5);// NOSONAR 2925
		assertTrue(f.isTimeQualified());
		assertTrue(f.canBeCallbacked());
		assertFalse(f.isNotYetMarkedAsDone());

		assertTrue(f.isDoneButChanged());
		assertEquals(f, f.resetDoneButChanged());
		assertFalse(f.isDoneButChanged());

		assertEquals(seeAgainFile, f.getLastFile());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
		verify(firstDetectionFile, atLeastOnce()).lastModified();
		verify(firstDetectionFile, atLeastOnce()).length();
		verify(seeAgainFile, atLeastOnce()).lastModified();
		verify(seeAgainFile, atLeastOnce()).length();
	}

	@Test
	void testUpdate_directory() {
		when(firstDetectionFile.isDirectory()).thenReturn(true);
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, Duration.ofDays(1));
		verify(firstDetectionFile, Mockito.times(2)).isDirectory();

		assertEquals(f, f.update(seeAgainFile));
		assertTrue(f.isTimeQualified());
		assertTrue(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());

		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_ONLY, ofDays(1));
		assertFalse(f.canBeCallbacked());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
	}

	@Test
	void testUpdate_directory_afterDone() {
		when(firstDetectionFile.isDirectory()).thenReturn(true);
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, Duration.ofDays(1));
		verify(firstDetectionFile, Mockito.times(2)).isDirectory();

		f.setMarkedAsDone();
		assertEquals(f, f.update(seeAgainFile));
		assertTrue(f.isTimeQualified());
		assertTrue(f.canBeCallbacked());
		assertFalse(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(firstDetectionFile, f.getLastFile());
	}

	@Test
	void testUpdate_updatedOnlySizes_OutTime() {
		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_DIRS, ZERO);

		when(firstDetectionFile.lastModified()).thenReturn(1L);
		when(seeAgainFile.lastModified()).thenReturn(1L);
		when(firstDetectionFile.length()).thenReturn(1L);
		when(seeAgainFile.length()).thenReturn(2L);

		assertEquals(f, f.update(seeAgainFile));

		verify(firstDetectionFile, times(1)).lastModified();
		verify(seeAgainFile, times(1)).lastModified();
		verify(firstDetectionFile, times(1)).length();
		verify(seeAgainFile, times(1)).length();

		assertFalse(f.isTimeQualified());
		assertFalse(f.canBeCallbacked());
		assertTrue(f.isNotYetMarkedAsDone());
		assertFalse(f.isDoneButChanged());
		assertEquals(seeAgainFile, f.getLastFile());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
	}

	@Test
	void testIsTimeQualified() {
		assertFalse(f.isTimeQualified());
	}

	@Test
	void testCanBePickup() {
		/** file + FILES_DIRS */
		assertTrue(f.canBePickupFromType());

		/** file */
		f = new WatchedFileInMemoryDb(firstDetectionFile, DIRS_ONLY, ZERO);
		assertFalse(f.canBePickupFromType());

		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_ONLY, ZERO);
		assertTrue(f.canBePickupFromType());

		/**
		 * dir
		 */
		when(firstDetectionFile.isDirectory()).thenReturn(true);

		f = new WatchedFileInMemoryDb(firstDetectionFile, DIRS_ONLY, ZERO);
		assertTrue(f.canBePickupFromType());

		f = new WatchedFileInMemoryDb(firstDetectionFile, FILES_ONLY, ZERO);
		assertFalse(f.canBePickupFromType());

		verify(firstDetectionFile, atLeastOnce()).isDirectory();
	}

	@Test
	void testSetMarkedAsDone() {
		f.setMarkedAsDone();
		assertFalse(f.isNotYetMarkedAsDone());
	}

	@Test
	void testIsNotYetMarkedAsDone() {
		assertTrue(f.isNotYetMarkedAsDone());
	}

	@Test
	void testAbsentInSet() {
		assertTrue(f.absentInSet(Set.of()));
		assertFalse(f.absentInSet(Set.of(firstDetectionFile)));
		assertTrue(f.absentInSet(Set.of(seeAgainFile)));
	}

	@Test
	void testToString() {
		assertNotNull(f.toString());
	}

	@Test
	void testDoneButChanged() {
		assertFalse(f.isDoneButChanged());
	}

	@Test
	void testResetDoneButChanged() {
		assertEquals(f, f.resetDoneButChanged());
		assertFalse(f.isDoneButChanged());
	}

}
