/*
 * This file is part of jobkit-watchfolder-jpa.
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
package tv.hd3g.jobkit.watchfolder.mod.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.transaction.Transactional;
import net.datafaker.Faker;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchFolderPickupType;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;
import tv.hd3g.jobkit.watchfolder.WatchedFileSetupManager;
import tv.hd3g.jobkit.watchfolder.WatchedFileSetupManager.WatchedFileScannerProviderEntry;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.mod.component.WatchedFileScannerSupplier;
import tv.hd3g.jobkit.watchfolder.mod.entity.WatchedFileEntity;
import tv.hd3g.jobkit.watchfolder.mod.repository.WatchedFileEntityRepository;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@SpringBootTest
class WatchedFilesDbServiceTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	WatchedFilesDbService s;
	@Autowired
	WatchedFileEntityRepository repository;
	@Value("${watchfolder.maxDeep:100}")
	int maxDeep;

	@Mock
	CachedFileAttributes file;
	@Mock
	CachedFileAttributes anotherFile;
	@Mock
	ObservedFolder observedFolder;
	@MockBean
	WatchedFileSetupManager watchedFileSetupManager;
	@MockBean
	WatchedFileScannerSupplier watchedFileScannerSupplier;
	@Mock
	WatchedFileScanner watchedFileScanner;
	@Mock
	AbstractFileSystemURL fileSystem;

	String label;
	String path;
	long length;
	long lastModified;
	WatchedFileEntity entity;
	WatchedFileScannerProviderEntry watchedFileScannerProviderEntry;
	WatchedFiles watchedFilesResult;

	@BeforeEach
	@Transactional
	void init() throws Exception {
		openMocks(this).close();
		repository.deleteAll();
		label = faker.numerify("label###");
		path = faker.numerify("/path###");
		length = Math.abs(faker.random().nextLong());
		lastModified = System.currentTimeMillis();

		when(observedFolder.getLabel()).thenReturn(label);
		when(file.getPath()).thenReturn(path);
		when(file.isDirectory()).thenReturn(faker.random().nextBoolean());
		when(file.length()).thenReturn(length);
		when(file.lastModified()).thenReturn(lastModified);

		when(anotherFile.getPath()).thenReturn(faker.numerify("/path###"));
		when(anotherFile.isDirectory()).thenReturn(faker.random().nextBoolean());
		when(anotherFile.length()).thenReturn(Math.abs(faker.random().nextLong()));
		when(anotherFile.lastModified()).thenReturn(System.currentTimeMillis());

		entity = new WatchedFileEntity(file, observedFolder);
		when(watchedFileScannerSupplier.create(observedFolder)).thenReturn(watchedFileScanner);
	}

	@AfterEach
	void end() {
		verify(observedFolder, atLeastOnce()).getLabel();
		verify(file, atLeastOnce()).getPath();
		verify(file, atLeastOnce()).isDirectory();
		verify(file, atLeastOnce()).length();
		verify(file, atLeastOnce()).lastModified();

		verifyNoMoreInteractions(
				file,
				anotherFile,
				observedFolder,
				watchedFileSetupManager,
				watchedFileScannerSupplier,
				fileSystem);
	}

	private int prepareAnotherEntityOutsideTest() {
		final var observedFolder = Mockito.mock(ObservedFolder.class);
		final var file = Mockito.mock(CachedFileAttributes.class);

		when(file.isDirectory()).thenReturn(false);
		when(observedFolder.getLabel()).thenReturn(faker.numerify("label###"));
		when(file.getPath()).thenReturn(faker.numerify("/path###"));
		when(file.isDirectory()).thenReturn(faker.random().nextBoolean());
		when(file.length()).thenReturn(Math.abs(faker.random().nextLong()));
		when(file.lastModified()).thenReturn(System.currentTimeMillis());

		final var anotherEntity = new WatchedFileEntity(file, observedFolder);
		final var beforeAdd = (int) repository.count();
		repository.saveAndFlush(anotherEntity);
		assertEquals(beforeAdd + 1, repository.count());
		return beforeAdd + 1;
	}

	private static Stream<Arguments> provideTestUpdateParameters() {
		return Stream.of(WatchFolderPickupType.values())
				.flatMap(p -> Stream.of(Arguments.of(p, true), Arguments.of(p, false)));
	}

	private static Stream<Arguments> provideTestUpdateLostedParameters() {
		return Stream.of(WatchFolderPickupType.values())
				.flatMap(p -> switch (p) {
				case FILES_ONLY -> Stream.of(Arguments.of(p, false));
				case DIRS_ONLY -> Stream.of(Arguments.of(p, true));
				case FILES_DIRS -> Stream.of(Arguments.of(p, true), Arguments.of(p, false));
				});
	}

	private boolean isCanUpdateCase(final WatchFolderPickupType pickUp, final boolean directory) {
		return pickUp.isPickUpFiles() && directory == false || pickUp.isPickUpDirs() && directory;
	}

	@ParameterizedTest
	@MethodSource("provideTestUpdateParameters")
	void testUpdate(final WatchFolderPickupType pickUp, final boolean directory) {
		final var repositorySize = prepareUpdate(pickUp, directory);

		/**
		 * First, add
		 */
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		if (isCanUpdateCase(pickUp, directory)) {
			assertEquals(1, watchedFilesResult.totalFiles());
			assertEquals(repositorySize + 1, repository.count());
		} else {
			assertEquals(0, watchedFilesResult.totalFiles());
			assertEquals(repositorySize + 1, repository.count());
			verify(observedFolder, times(1)).getMinFixedStateTime();
			verify(watchedFileSetupManager, times(1)).get(observedFolder);
			verify(watchedFileScanner, times(1)).scan(fileSystem);
			return;
		}

		/**
		 * Second, update
		 */
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertEquals(1, watchedFilesResult.founded().size());
		assertEquals(file, watchedFilesResult.founded().stream().findFirst().get());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		/**
		 * Third, nothing new
		 */
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		/**
		 * Fourth, remove deleted
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of());
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());

		assertEquals(0, watchedFilesResult.totalFiles());
		assertEquals(repositorySize, repository.count());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileSetupManager, atLeastOnce()).get(observedFolder);
		verify(watchedFileScanner, times(4)).scan(fileSystem);
	}

	@ParameterizedTest
	@MethodSource("provideTestUpdateLostedParameters")
	void testUpdate_lost(final WatchFolderPickupType pickUp, final boolean directory) {
		final var repositorySize = prepareUpdate(pickUp, directory);

		/**
		 * First, add
		 */
		s.update(observedFolder, fileSystem);

		/**
		 * Second, remove added
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of());
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());

		assertEquals(1, watchedFilesResult.losted().size());
		final var losted = watchedFilesResult.losted().stream().findFirst().get();
		assertFalse(losted.exists());
		assertEquals(directory, losted.isDirectory());
		assertEquals(path, losted.getPath());
		assertEquals(lastModified, losted.lastModified());
		assertEquals(length, losted.length());

		assertEquals(0, watchedFilesResult.totalFiles());
		assertEquals(repositorySize, repository.count());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileSetupManager, atLeastOnce()).get(observedFolder);
		verify(watchedFileScanner, times(2)).scan(fileSystem);
	}

	@ParameterizedTest
	@MethodSource("provideTestUpdateLostedParameters")
	void testUpdate_lost_nonEmptyDir(final WatchFolderPickupType pickUp, final boolean directory) {
		final var repositorySize = prepareUpdate(pickUp, directory);
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(file, anotherFile));

		/**
		 * First, add
		 */
		s.update(observedFolder, fileSystem);

		/**
		 * Second, remove added
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(anotherFile));
		watchedFilesResult = s.update(observedFolder, fileSystem);
		assertEquals(1, watchedFilesResult.founded().size());
		assertEquals(anotherFile, watchedFilesResult.founded().stream().findFirst().get());

		assertEquals(1, watchedFilesResult.losted().size());
		final var losted = watchedFilesResult.losted().stream().findFirst().get();
		assertFalse(losted.exists());
		assertEquals(directory, losted.isDirectory());
		assertEquals(path, losted.getPath());
		assertEquals(lastModified, losted.lastModified());
		assertEquals(length, losted.length());

		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileSetupManager, atLeastOnce()).get(observedFolder);
		verify(watchedFileScanner, times(2)).scan(fileSystem);

		verify(anotherFile, atLeastOnce()).getPath();
		verify(anotherFile, atLeastOnce()).isDirectory();
		verify(anotherFile, atLeastOnce()).length();
		verify(anotherFile, atLeastOnce()).lastModified();
	}

	@Test
	void testPurgeOlderWF_empty() {
		when(observedFolder.getLabel()).thenReturn(label);
		s.purgeOlderWF(Set.of(observedFolder));
		assertEquals(0, repository.count());
	}

	@Test
	void testPurgeOlderWF() {
		repository.saveAndFlush(entity);
		s.purgeOlderWF(Set.of(observedFolder));
		assertEquals(1, repository.count());

		when(observedFolder.getLabel()).thenReturn(faker.numerify("label###"));
		s.purgeOlderWF(Set.of(observedFolder));
		assertEquals(0, repository.count());
	}

	@Test
	void testPurgeOlderWF_noItems() {
		assertDoesNotThrow(() -> s.purgeOlderWF(Set.of()));
	}

	@Test
	void testSetup() {
		final var pickUp = faker.options().option(WatchFolderPickupType.class);
		s.setup(observedFolder, pickUp);
		verify(watchedFileSetupManager, times(1)).put(observedFolder, watchedFileScanner, pickUp);
		verify(watchedFileScannerSupplier, times(1)).create(observedFolder);
		assertEquals(0, repository.count());
	}

	@Test
	void testReset() {
		s.reset(observedFolder, Set.of(file));
		assertEquals(0, repository.count());

		repository.saveAndFlush(entity);
		s.reset(observedFolder, Set.of(file));
		assertEquals(0, repository.count());
	}

	@Test
	void testReset_noItems() {
		final Set<CachedFileAttributes> emptySet = Set.of();
		assertThrows(IllegalArgumentException.class, () -> s.reset(observedFolder, emptySet));
	}

	private int prepareUpdate(final WatchFolderPickupType pickUp, final boolean directory) {
		final var repositorySize = prepareAnotherEntityOutsideTest();
		watchedFileScannerProviderEntry = new WatchedFileScannerProviderEntry(watchedFileScanner, pickUp);
		when(file.isDirectory()).thenReturn(directory);
		when(anotherFile.isDirectory()).thenReturn(directory);
		when(watchedFileSetupManager.get(observedFolder)).thenReturn(watchedFileScannerProviderEntry);
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(file));
		when(observedFolder.getMinFixedStateTime()).thenReturn(Duration.ofMillis(-1000));
		return repositorySize;
	}

}
