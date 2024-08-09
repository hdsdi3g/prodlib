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

import static java.lang.Math.abs;
import static java.time.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.IGNORE_FOUNDED_FILE;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;
import static tv.hd3g.jobkit.watchfolder.Watchfolders.DEFAULT_RETRY_AFTER_TIME;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.InvalidURLException;

@ExtendWith(MockToolsExtendsJunit.class)
class WatchfoldersTest {
	static final Faker faker = Faker.instance();

	@Mock
	FolderActivity folderActivity;
	@Mock
	WatchedFilesDb watchedFilesDb;
	@Mock
	WatchFolderPickupType pickUp;
	@Mock
	WatchedFiles watchedFiles;

	@Fake(min = 2, max = 5)
	int numberManualQueuesToTest;

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
		when(watchedFilesDb.update(eq(observedFolder), any(AbstractFileSystemURL.class))).thenReturn(watchedFiles);
	}

	@Test
	void testDisabledFolder() {
		observedFolder.setDisabled(true);
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		observedFolder.setTargetFolder("file://localhost/" + new File("").getAbsolutePath());
		watchfolders.startScans();
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
	}

	@Test
	void testNotSameLabels() {
		final var observedFolder2 = new ObservedFolder();
		observedFolder2.setLabel(observedFolder.getLabel());
		final var allObservedFolders = List.of(observedFolder, observedFolder2);
		final Supplier<WatchedFilesDb> watchedFilesDbBuilder = () -> watchedFilesDb;
		assertThrows(IllegalArgumentException.class,
				() -> new Watchfolders(allObservedFolders,
						folderActivity,
						ZERO, jobKitEngine, "default", "default", watchedFilesDbBuilder));
	}

	@Test
	void testMissingActiveFolder_duringRun() throws IOException {
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		verify(folderActivity, times(0)).onScanErrorFolder(any(ObservedFolder.class), any(Exception.class));

		observedFolder.setTargetFolder("file://localhost/this/not/exists");
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		watchfolders.startScans();

		assertThrows(IllegalStateException.class, () -> jobKitEngine.runAllServicesOnce());
		verify(folderActivity, times(1)).onScanErrorFolder(eq(observedFolder), any(InvalidURLException.class));
		assertFalse(jobKitEngine.isEmptyActiveServicesList());

		assertThrows(IllegalStateException.class, () -> jobKitEngine.runAllServicesOnce());
		verify(folderActivity, times(2)).onScanErrorFolder(eq(observedFolder), any(InvalidURLException.class));
		assertFalse(jobKitEngine.isEmptyActiveServicesList());

		assertThrows(IllegalStateException.class, () -> jobKitEngine.runAllServicesOnce());
		verify(folderActivity, times(3)).onScanErrorFolder(eq(observedFolder), any(InvalidURLException.class));

		/**
		 * Back to normal
		 */
		observedFolder.setTargetFolder("file://localhost/" + new File("").getAbsolutePath());
		jobKitEngine.runAllServicesOnce();
		verify(folderActivity, times(1)).onStartScan(observedFolder);

		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(folderActivity, atMostOnce()).onBeforeScan(observedFolder);
		verify(folderActivity, atMostOnce()).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(watchedFilesDb, atMostOnce()).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068
	}

	@Test
	void testStartStopScans() throws IOException {// NOSONAR 5961
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);
		assertTrue(jobKitEngine.isEmptyActiveServicesList());

		watchfolders.startScans();
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onStartScan(observedFolder);
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(1)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(0)).onStopScan(observedFolder);

		watchfolders.stopScans();
		jobKitEngine.runAllServicesOnce();

		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onStartScan(observedFolder);
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(1)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).onStopScan(observedFolder);

		watchfolders.startScans();
		watchfolders.startScans();
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onStartScan(observedFolder);
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).onStopScan(observedFolder);

		watchfolders.stopScans();
		watchfolders.stopScans();
		jobKitEngine.runAllServicesOnce();

		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onStartScan(observedFolder);
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(3)).onStopScan(observedFolder);

		verify(folderActivity, times(0)).onScanErrorFolder(eq(observedFolder), any(Exception.class));

		verify(folderActivity, times(1)).getPickUpType(observedFolder);

		verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068
	}

	@Test
	void test_OnFolderActivity_Error() throws IOException {
		final var cfa = Mockito.mock(CachedFileAttributes.class);
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		watchfolders.startScans();

		doThrow(UncheckedIOException.class)
				.when(folderActivity).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onStartScan(observedFolder);
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(watchedFilesDb, times(1)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(0)).reset(eq(observedFolder), any());
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).retryScanPolicyOnUserError(
				eq(observedFolder), any(), any(UncheckedIOException.class));
		verify(watchedFiles, times(1)).founded();

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(2)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(0)).reset(eq(observedFolder), any());
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(2)).retryScanPolicyOnUserError(
				eq(observedFolder), any(), any(UncheckedIOException.class));
		verify(watchedFiles, times(2)).founded();

		when(watchedFiles.founded()).thenReturn(Set.of(cfa));
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(3)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(3)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(1)).reset(observedFolder, Set.of(cfa));
		verify(folderActivity, times(3)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(3)).retryScanPolicyOnUserError(
				eq(observedFolder), any(), any(UncheckedIOException.class));
		verify(watchedFiles, times(3)).founded();

		when(folderActivity.getPickUpType(observedFolder)).thenReturn(pickUp);

		jobKitEngine.runAllServicesOnce();

		verify(folderActivity, times(4)).onBeforeScan(observedFolder);
		verify(watchedFilesDb, times(4)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(2)).reset(observedFolder, Set.of(cfa));
		verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068
		verify(folderActivity, times(4)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(4)).retryScanPolicyOnUserError(
				eq(observedFolder), any(), any(UncheckedIOException.class));
		verify(watchedFiles, times(4)).founded();

		verifyNoInteractions(cfa);
	}

	@Test
	void test_OnFolderActivity_Error_NoRetryfile() throws IOException {
		final var cfa = Mockito.mock(CachedFileAttributes.class);
		when(folderActivity.retryScanPolicyOnUserError(any(ObservedFolder.class),
				eq(watchedFiles), any(Exception.class)))
						.thenReturn(IGNORE_FOUNDED_FILE);

		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		watchfolders.startScans();

		doThrow(UncheckedIOException.class)
				.when(folderActivity).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));

		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(1)).onBeforeScan(observedFolder);
		verify(folderActivity, times(1)).onStartScan(observedFolder);
		verify(folderActivity, times(1)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(1)).getPickUpType(observedFolder);
		verify(watchedFilesDb, times(1)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
		verify(watchedFilesDb, times(1)).setup(observedFolder, pickUp);
		verify(watchedFiles, times(1)).founded();

		when(watchedFiles.founded()).thenReturn(Set.of(cfa));
		jobKitEngine.runAllServicesOnce();

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		verify(folderActivity, times(2)).onBeforeScan(observedFolder);
		verify(folderActivity, times(2)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));
		verify(folderActivity, times(2)).retryScanPolicyOnUserError(
				eq(observedFolder), any(), any(UncheckedIOException.class));
		verify(watchedFiles, times(2)).founded();
		verify(watchedFilesDb, times(2)).update(eq(observedFolder), any(AbstractFileSystemURL.class));

		verifyNoInteractions(cfa);
	}

	@Nested
	class ApplyWFSetups {

		String defaultSpoolScans;
		String defaultSpoolEvents;
		Duration defaultTimeBetweenScans;
		String spoolScans;
		String spoolEvents;
		Duration timeBetweenScans;
		int retryAfterTimeFactor;
		BackgroundService service;
		int priority;

		@BeforeEach
		void init() {
			defaultSpoolScans = faker.numerify("defaultSpoolScans###");
			defaultSpoolEvents = faker.numerify("defaultSpoolEvents###");
			defaultTimeBetweenScans = Duration.ofMillis(abs(faker.random().nextInt()));
			spoolScans = faker.numerify("spoolScans###");
			spoolEvents = faker.numerify("spoolEvents###");
			timeBetweenScans = Duration.ofMillis(abs(faker.random().nextInt()));
			retryAfterTimeFactor = abs(faker.random().nextInt());
			priority = abs(faker.random().nextInt());
		}

		@AfterEach
		void close() {
			assertTrue(jobKitEngine.isEmptyActiveServicesList());
			reset(folderActivity, watchedFilesDb);
		}

		void checkService() {
			final var observedFoldersServices = watchfolders.getObservedFoldersServices();
			assertNotNull(observedFoldersServices);
			assertEquals(1, observedFoldersServices.size());
			assertEquals(observedFolder, observedFoldersServices.keySet().stream().findAny().get());
			service = observedFoldersServices.get(observedFolder);
			assertNotNull(service);
			assertFalse(service.isEnabled());
		}

		@Test
		void testByDefault() {
			watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
					defaultTimeBetweenScans, jobKitEngine, defaultSpoolScans, defaultSpoolEvents, () -> watchedFilesDb);
			checkService();
			assertEquals(0, service.getPriority());
			assertEquals(DEFAULT_RETRY_AFTER_TIME, service.getRetryAfterTimeFactor());
			assertEquals(defaultTimeBetweenScans, service.getTimedIntervalDuration());
			assertEquals(defaultSpoolScans, service.getSpoolName());

			assertEquals(defaultSpoolScans, observedFolder.getSpoolScans());
			assertEquals(defaultSpoolEvents, observedFolder.getSpoolEvents());
			assertEquals(defaultTimeBetweenScans, observedFolder.getTimeBetweenScans());
			assertEquals(DEFAULT_RETRY_AFTER_TIME, observedFolder.getRetryAfterTimeFactor());
			assertEquals(0, observedFolder.getJobsPriority());
		}

		@Test
		void testSetDefault_empty() {
			observedFolder.setSpoolEvents("");
			observedFolder.setSpoolScans("");
			observedFolder.setTimeBetweenScans(ZERO);
			observedFolder.setRetryAfterTimeFactor(-1);

			watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
					defaultTimeBetweenScans, jobKitEngine, defaultSpoolScans, defaultSpoolEvents, () -> watchedFilesDb);
			checkService();
			assertEquals(0, service.getPriority());
			assertEquals(DEFAULT_RETRY_AFTER_TIME, service.getRetryAfterTimeFactor());
			assertEquals(defaultTimeBetweenScans, service.getTimedIntervalDuration());
			assertEquals(defaultSpoolScans, service.getSpoolName());

			assertEquals(defaultSpoolScans, observedFolder.getSpoolScans());
			assertEquals(defaultSpoolEvents, observedFolder.getSpoolEvents());
			assertEquals(defaultTimeBetweenScans, observedFolder.getTimeBetweenScans());
			assertEquals(DEFAULT_RETRY_AFTER_TIME, observedFolder.getRetryAfterTimeFactor());
			assertEquals(0, observedFolder.getJobsPriority());
		}

		@Test
		void testSetup() {
			observedFolder.setSpoolEvents(spoolEvents);
			observedFolder.setSpoolScans(spoolScans);
			observedFolder.setTimeBetweenScans(timeBetweenScans);
			observedFolder.setRetryAfterTimeFactor(retryAfterTimeFactor);
			observedFolder.setJobsPriority(priority);

			watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
					defaultTimeBetweenScans, jobKitEngine, defaultSpoolScans, defaultSpoolEvents, () -> watchedFilesDb);
			checkService();
			assertEquals(priority, service.getPriority());
			assertEquals(retryAfterTimeFactor, service.getRetryAfterTimeFactor());
			assertEquals(timeBetweenScans, service.getTimedIntervalDuration());
			assertEquals(spoolScans, service.getSpoolName());
			assertEquals(priority, observedFolder.getJobsPriority());
		}

	}

	@Test
	void testQueueManualScan() throws IOException {// NOSONAR 5961
		watchfolders = new Watchfolders(List.of(observedFolder), folderActivity,
				Duration.ofMillis(1), jobKitEngine, "default", "default", () -> watchedFilesDb);

		for (var pos = 0; pos < numberManualQueuesToTest; pos++) {
			watchfolders.queueManualScan();
			final var count = pos + 1;
			assertTrue(jobKitEngine.isEmptyActiveServicesList());
			verify(folderActivity, times(count)).onBeforeScan(observedFolder);
			verify(watchedFilesDb, times(count)).update(eq(observedFolder), any(AbstractFileSystemURL.class));
			verify(folderActivity, times(count)).onAfterScan(eq(observedFolder), any(Duration.class), eq(watchedFiles));

			verify(folderActivity, times(1)).getPickUpType(observedFolder);
			verify(watchedFilesDb, times(1)).setup(eq(observedFolder), eq(pickUp));// NOSONAR S6068*/
		}

		jobKitEngine.runAllServicesOnce();
	}

}
