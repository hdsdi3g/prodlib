/*
 * This file is part of jobkit-watchfolder.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.watchfolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;

class WatchfoldersErrorTest {

	static Faker faker = net.datafaker.Faker.instance();

	@Mock
	FolderActivity folderActivity;
	@Mock
	WatchedFilesDb watchedFilesDb;
	@Mock
	WatchFolderPickupType pickUp;
	@Mock
	WatchedFiles watchedFiles;
	@Mock
	ObservedFolder observedFolder;

	FlatJobKitEngine jobKitEngine;
	Watchfolders watchfolders;
	AbstractFileSystemURL url;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		when(observedFolder.getLabel()).thenReturn("Internal mock test");
		jobKitEngine = new FlatJobKitEngine();

		when(folderActivity.getPickUpType(observedFolder)).thenReturn(pickUp);
		when(folderActivity.retryScanPolicyOnUserError(any(ObservedFolder.class),
				eq(watchedFiles), any(Exception.class)))
						.thenReturn(RETRY_FOUNDED_FILE);
		when(watchedFilesDb.update(eq(observedFolder), any(AbstractFileSystemURL.class))).thenReturn(watchedFiles);
		url = new AbstractFileSystemURL("file://localhost/" + new File("").getAbsolutePath());
	}

	@Test
	void test_OnFolderActivityRetry() throws IOException {
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		watchfolders.startScans();

		final var e = new UncheckedIOException(new IOException("Bad folder scan (this is ok)"));
		doThrow(e).when(observedFolder).createFileSystem();

		jobKitEngine.runAllServicesOnce();

		final var retryService = watchfolders.getOnErrorObservedFolders().get(observedFolder);
		assertNotNull(retryService);
		assertEquals(1, watchfolders.getOnErrorObservedFolders().size());
		assertTrue(retryService.isEnabled());
		assertFalse(retryService.isHasFirstStarted());

		verify(folderActivity, times(1)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(folderActivity, times(1)).onScanErrorFolder(observedFolder, e);
		verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068
		verifyNoMoreInteractions(folderActivity, watchedFilesDb, watchedFiles);

		assertThrows(IllegalStateException.class, () -> jobKitEngine.runAllServicesOnce());
		try {
			jobKitEngine.runAllServicesOnce();
		} catch (final Exception ee) {
			assertEquals(e, ee.getCause());
		}

		assertEquals(retryService, watchfolders.getOnErrorObservedFolders().get(observedFolder));
		assertEquals(1, watchfolders.getOnErrorObservedFolders().size());
		assertTrue(retryService.isEnabled());
		assertFalse(retryService.isHasFirstStarted());

		verifyNoMoreInteractions(folderActivity, watchedFilesDb, watchedFiles);

		reset(observedFolder);
		when(observedFolder.createFileSystem()).thenReturn(url);

		jobKitEngine.runAllServicesOnce();

		assertFalse(retryService.isEnabled());
		assertEquals(0, watchfolders.getOnErrorObservedFolders().size());
		verify(folderActivity, times(1)).onStopRetryOnError(observedFolder);
		verify(folderActivity, atMost(1)).onBeforeScan(observedFolder);
		verify(folderActivity, atMost(1)).onAfterScan(eq(observedFolder), any(), any());
		verifyNoMoreInteractions(folderActivity);

		jobKitEngine.runAllServicesOnce();
		assertFalse(retryService.isEnabled());
		assertEquals(0, watchfolders.getOnErrorObservedFolders().size());
		verify(folderActivity, atMost(2)).onBeforeScan(observedFolder);
		verify(folderActivity, atMost(2)).onAfterScan(eq(observedFolder), any(), any());
		verifyNoMoreInteractions(folderActivity);
	}

}
