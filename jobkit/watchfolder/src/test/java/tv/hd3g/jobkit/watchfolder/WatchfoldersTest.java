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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.engine.flat.FlatJobKitEngine;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

class WatchfoldersTest {

	@Mock
	FolderActivity folderActivity;
	@Mock
	WatchedFilesDb watchedFilesDb;
	@Mock
	WatchFolderPickupType pickUp;
	@Mock
	WatchedFiles watchedFiles;

	ObservedFolder observedFolder;
	FlatJobKitEngine jobKitEngine;

	Watchfolders watchfolders;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		observedFolder = new ObservedFolder();
		observedFolder.setLabel("Internal test");
		observedFolder.setTargetFolder("file://localhost/" + new File("").getAbsolutePath());
		jobKitEngine = new FlatJobKitEngine();

		when(folderActivity.getPickUpType(observedFolder)).thenReturn(pickUp);
		when(folderActivity.retryScanPolicyOnUserError(any(ObservedFolder.class),
		        eq(watchedFiles), any(Exception.class)))
		                .thenReturn(RETRY_FOUNDED_FILE);
		when(watchedFilesDb.update(any(AbstractFileSystemURL.class))).thenReturn(watchedFiles);
	}

	@AfterEach
	void close() throws InterruptedException {
		verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068
		verifyNoMoreInteractions(folderActivity, watchedFilesDb, pickUp, watchedFiles);
	}

	@Test
	void testMissingActiveFolder_duringRun() {
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
		        Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		verify(folderActivity, times(0)).onScanErrorFolder(any(ObservedFolder.class), any(Exception.class));

		observedFolder.setTargetFolder("file://localhost/this/not/exists");
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		watchfolders.startScans();

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onScanErrorFolder(eq(observedFolder),
		        argThat(f -> f.getCause() instanceof NoSuchFileException));

		assertThrows(UncheckedIOException.class, () -> jobKitEngine.runAllServicesOnce());

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onScanErrorFolder(eq(observedFolder), any(Exception.class));

		assertThrows(UncheckedIOException.class, () -> jobKitEngine.runAllServicesOnce());

		/**
		 * Back to normal
		 */
		observedFolder.setTargetFolder("file://localhost/" + new File("").getAbsolutePath());
		jobKitEngine.runAllServicesOnce();
		verify(folderActivity, times(1)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(1)).onScanErrorFolder(eq(observedFolder), any(Exception.class));

		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(folderActivity, atMostOnce()).onBeforeScan(observedFolder);
		verify(folderActivity, atMostOnce()).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFilesDb, atMostOnce()).update(any(AbstractFileSystemURL.class));
	}

	@Test
	void testStartStopScans() {// NOSONAR 5961
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
		        Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);
		assertTrue(jobKitEngine.isEmptyActiveServicesList());

		watchfolders.startScans();
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(1)).update(any(AbstractFileSystemURL.class));
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(0)).onStopScans(List.of(observedFolder));

		watchfolders.stopScans();
		jobKitEngine.runAllServicesOnce();

		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(1)).update(any(AbstractFileSystemURL.class));
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).onStopScans(List.of(observedFolder));

		watchfolders.startScans();
		watchfolders.startScans();
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(any(AbstractFileSystemURL.class));
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).onStopScans(List.of(observedFolder));

		watchfolders.stopScans();
		watchfolders.stopScans();
		jobKitEngine.runAllServicesOnce();

		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(any(AbstractFileSystemURL.class));
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(2)).onStopScans(List.of(observedFolder));

		verify(folderActivity, times(0)).onScanErrorFolder(any(ObservedFolder.class), any(Exception.class));

		verify(folderActivity, times(1)).getPickUpType(observedFolder);
	}

	@Test
	void testGetService() {
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
		        Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);
		assertNull(watchfolders.getService());

		watchfolders.startScans();
		assertNotNull(watchfolders.getService());
		watchfolders.stopScans();
		assertNull(watchfolders.getService());

		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(folderActivity, times(1)).onStartScans(List.of(observedFolder));
		verify(folderActivity, times(1)).onStopScans(List.of(observedFolder));
	}

	@Test
	void test_OnFolderActivity_Error() {
		final var cfa = Mockito.mock(CachedFileAttributes.class);
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
		        Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		watchfolders.startScans();

		doThrow(UncheckedIOException.class)
		        .when(folderActivity).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(1)).update(any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(0)).reset(any());
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFiles, times(1)).getFounded();

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(0)).reset(any());
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFiles, times(2)).getFounded();

		when(watchedFiles.getFounded()).thenReturn(Set.of(cfa));
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(3)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(3)).update(any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(1)).reset(Set.of(cfa));
		verify(folderActivity, times(3)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFiles, times(3)).getFounded();

		reset(folderActivity);
		when(folderActivity.getPickUpType(observedFolder)).thenReturn(pickUp);

		jobKitEngine.runAllServicesOnce();

		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(4)).update(any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(1)).reset(Set.of(cfa));
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFiles, times(3)).getFounded();

		verifyNoInteractions(cfa);
	}

}
