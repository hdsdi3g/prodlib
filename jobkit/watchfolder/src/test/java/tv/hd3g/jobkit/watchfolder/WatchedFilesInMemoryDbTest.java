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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.DIRS_ONLY;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_DIRS;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_ONLY;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.local.LocalFile;

class WatchedFilesInMemoryDbTest {

	static final File rootDir = new File("target/test-" + WatchedFilesInMemoryDbTest.class.getSimpleName());

	@BeforeAll
	static void prepare() throws IOException {
		FileUtils.forceMkdir(rootDir);
		FileUtils.cleanDirectory(rootDir);
	}

	File workingFile;
	ObservedFolder observedFolder;
	WatchedFilesInMemoryDb watchedFilesDb;
	AbstractFileSystemURL fs;

	@BeforeEach
	void init() throws IOException {
		observedFolder = new ObservedFolder();
		workingFile = new File(rootDir, String.valueOf(Math.abs(System.nanoTime())));
		observedFolder.setTargetFolder("file://localhost/" + workingFile.getAbsolutePath());
		if (workingFile.exists()) {
			throw new IOException("Temp dir exists: " + workingFile);
		}
		FileUtils.forceMkdir(workingFile);

		observedFolder.setLabel("test");
		observedFolder.setAllowedExtentions(Set.of("ok", "ok.long"));
		observedFolder.setBlockedExtentions(Set.of("no", "no.long"));
		observedFolder.setIgnoreRelativePaths(Set.of("never/here"));
		observedFolder.setIgnoreFiles(Set.of("desktop.ini", ".DS_Store", "ignoreme.ok"));

		watchedFilesDb = new WatchedFilesInMemoryDb();
		fs = observedFolder.createFileSystem();
	}

	@Test
	void testUpdate_disabled() {
		observedFolder.setDisabled(true);
		assertThrows(IllegalArgumentException.class, () -> watchedFilesDb.setup(observedFolder, FILES_ONLY));
		assertThrows(IllegalArgumentException.class, () -> watchedFilesDb.setup(observedFolder, FILES_DIRS));
		assertThrows(IllegalArgumentException.class, () -> watchedFilesDb.setup(observedFolder, DIRS_ONLY));
	}

	@Test
	void testUpdate_found() {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 0);

		write("thisfine.ok", "thisfine.ok.long", "thisnotfine.no",
				"thisnotfine.no.long", "/whysubdir/thisfine.ok", "ignoreme.ok");

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok", "thisfine.ok.long"), Set.of(), 2);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);

		delete("thisfine.ok");
		delete("thisfine.ok.long");
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 0);
	}

	@Test
	void testUpdate_updates_size() {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine2.ok");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);

		write("thisfine2.ok");
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);

		write("thisfine2.ok");
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine2.ok"), Set.of(), 1);
	}

	@Test
	void testUpdate_updates_dates() throws IOException, InterruptedException {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine5.ok");
		final var thisfine5 = toAbsolutePath("thisfine5.ok");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);

		Thread.sleep(100);// NOSONAR
		FileUtils.touch(thisfine5);
		final var firstDate = thisfine5.lastModified();
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);

		Thread.sleep(100);// NOSONAR
		FileUtils.touch(thisfine5);
		final var lastDate = thisfine5.lastModified();

		if (lastDate != firstDate) {
			/**
			 * This filesystem can't handle dates at ms precision
			 */
			w = watchedFilesDb.update(fs);
			assertWatchedFiles(w, Set.of(), Set.of(), 1);
		}

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine5.ok"), Set.of(), 1);
	}

	@Test
	void testUpdate_lost() {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine3.ok");
		var w = watchedFilesDb.update(fs);

		delete("thisfine3.ok");
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of("thisfine3.ok"), 0);
	}

	@Test
	void testUpdate_subdir() {
		observedFolder.setRecursive(true);
		watchedFilesDb.setup(observedFolder, FILES_ONLY);
		assertEquals(10, watchedFilesDb.getMaxDeep());

		write("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok");

		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 3);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w,
				Set.of("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok"),
				Set.of(), 3);
	}

	@Test
	void testUpdate_onlyDir() {
		observedFolder.setRecursive(true);
		watchedFilesDb.setup(observedFolder, DIRS_ONLY);
		assertEquals(10, watchedFilesDb.getMaxDeep());

		write("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok");

		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 4);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w,
				Set.of("/oksubdir", "/sub", "/sub/sub", "/sub/sub/dir"),
				Set.of(), 4);
	}

	@Test
	void testUpdate_FilesDir() {
		observedFolder.setRecursive(true);
		watchedFilesDb.setup(observedFolder, FILES_DIRS);
		assertEquals(10, watchedFilesDb.getMaxDeep());

		write("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok");

		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 4 + 3);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w,
				Set.of("/oksubdir", "/sub", "/sub/sub", "/sub/sub/dir",
						"thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok"),
				Set.of(), 4 + 3);
	}

	@Test
	void testUpdate_hidden() {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine.ok", ".thisnotfine.ok");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok"), Set.of(), 1);

		observedFolder.setAllowedHidden(true);
		watchedFilesDb = new WatchedFilesInMemoryDb();
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok", ".thisnotfine.ok"), Set.of(), 2);
	}

	@Test
	void testUpdate_noExtentionsMgm() {
		observedFolder.setAllowedExtentions(null);
		observedFolder.setBlockedExtentions(null);
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine"), Set.of(), 1);
	}

	@Test
	void testUpdate_relativePaths() {
		observedFolder.setRecursive(true);
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine.ok", "/never/here/thisfine.ok");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 1);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok"), Set.of(), 1);

		observedFolder.setIgnoreRelativePaths(null);
		watchedFilesDb = new WatchedFilesInMemoryDb();
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok", "/never/here/thisfine.ok"), Set.of(), 2);
	}

	private File toAbsolutePath(final String relativePath) {
		return new File(workingFile, relativePath).getAbsoluteFile();
	}

	private Set<File> toAbsolutePath(final Collection<String> relativePaths) {
		return relativePaths.stream()
				.map(this::toAbsolutePath)
				.collect(toUnmodifiableSet());
	}

	private void write(final String... relativePath) {
		for (var pos = 0; pos < relativePath.length; pos++) {
			write(relativePath[pos]);
		}
	}

	private void write(final String relativePath) {
		try {
			final var f = toAbsolutePath(relativePath);
			FileUtils.forceMkdirParent(f);
			Files.write(f.toPath(), Set.of(String.valueOf(System.nanoTime())), APPEND, SYNC, WRITE, CREATE);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void delete(final String relativePath) {
		try {
			final var f = toAbsolutePath(relativePath);
			if (f.exists()) {
				FileUtils.forceDelete(f);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void assertWatchedFiles(final WatchedFiles watchedFiles,
									final Set<String> founded,
									final Set<String> losted,
									final int totalFiles) {
		assertNotNull(watchedFiles);
		final var currentRootDir = workingFile.getName();
		assertEquals(toAbsolutePath(founded), revealRealFiles(watchedFiles.founded()),
				"Search founded; root dir: " + currentRootDir);
		assertEquals(toAbsolutePath(losted), revealRealFiles(watchedFiles.losted()),
				"Search losted; root dir: " + currentRootDir);
		assertEquals(totalFiles, watchedFiles.totalFiles(),
				"Search totalFiles; root dir: " + currentRootDir);

		try {
			Thread.sleep(1);// NOSONAR S2925
		} catch (final InterruptedException e) {
		}
	}

	private Set<File> revealRealFiles(final Set<CachedFileAttributes> fileAttr) {
		return fileAttr.stream()
				.map(CachedFileAttributes::getAbstractFile)
				.map(af -> (LocalFile) af)
				.map(LocalFile::getInternalFile)
				.collect(toUnmodifiableSet());
	}

	@Test
	void testSetup() {
		observedFolder.setRecursive(false);
		watchedFilesDb.setup(observedFolder, FILES_DIRS);
		assertEquals(0, watchedFilesDb.getMaxDeep());

		observedFolder.setRecursive(true);
		watchedFilesDb = new WatchedFilesInMemoryDb();
		watchedFilesDb.setup(observedFolder, FILES_DIRS);
		assertEquals(10, watchedFilesDb.getMaxDeep());
	}

	@Test
	void testGetMaxDeep() {
		assertEquals(10, watchedFilesDb.getMaxDeep());
	}

	@Test
	void testSetMaxDeep() {
		watchedFilesDb.setMaxDeep(5);
		assertEquals(5, watchedFilesDb.getMaxDeep());
	}

	@Test
	void testReset() {
		watchedFilesDb.setup(observedFolder, FILES_ONLY);

		write("thisfine.ok", "thisfine.ok.long", "thisnotfine.no", "thisnotfine.no.long",
				"/whysubdir/thisfine.ok", "ignoreme.ok");
		var w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok", "thisfine.ok.long"), Set.of(), 2);

		watchedFilesDb.reset(w.founded());

		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of("thisfine.ok", "thisfine.ok.long"), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
		w = watchedFilesDb.update(fs);
		assertWatchedFiles(w, Set.of(), Set.of(), 2);
	}

	@Nested
	class AllowedBlocked {
		@BeforeEach
		void init() throws IOException {
			observedFolder.setRecursive(true);
		}

		@Test
		void setAllowedDirNames() {
			observedFolder.setAllowedDirNames(Set.of("ok???dir", "something.else"));
			watchedFilesDb.setup(observedFolder, FILES_ONLY);

			write("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok");

			var w = watchedFilesDb.update(fs);
			assertWatchedFiles(w, Set.of(), Set.of(), 2);

			w = watchedFilesDb.update(fs);
			assertWatchedFiles(w,
					Set.of("thisfine.ok", "/oksubdir/thisfine.ok"),
					Set.of(), 2);
		}

		@Test
		void setBlockedDirNames() {
			observedFolder.setBlockedDirNames(Set.of("ok???dir", "something.else"));
			watchedFilesDb.setup(observedFolder, FILES_ONLY);

			write("thisfine.ok", "/oksubdir/thisfine.ok", "/sub/sub/dir/anotherfine.ok");

			var w = watchedFilesDb.update(fs);
			assertWatchedFiles(w, Set.of(), Set.of(), 2);

			w = watchedFilesDb.update(fs);
			assertWatchedFiles(w,
					Set.of("thisfine.ok", "/sub/sub/dir/anotherfine.ok"),
					Set.of(), 2);
		}

		@Test
		void setAllowedFileNames() {
			observedFolder.setAllowedFileNames(Set.of("*fine.ok", "something.else"));
			watchedFilesDb.setup(observedFolder, FILES_ONLY);

			write("/oksubdir/thisfine.ok", "/oksubdir2/thisfine.ko", "/sub/sub/dir/fined.ok");

			var w = watchedFilesDb.update(fs);
			assertWatchedFiles(w, Set.of(), Set.of(), 1);

			w = watchedFilesDb.update(fs);
			assertWatchedFiles(w,
					Set.of("/oksubdir/thisfine.ok"),
					Set.of(), 1);
		}

		@Test
		void setBlockedFileNames() {
			observedFolder.setBlockedFileNames(Set.of("*fine.ok", "something.else"));
			watchedFilesDb.setup(observedFolder, FILES_ONLY);

			write("/oksubdir/thisfine.ok", "/oksubdir2/thisfine.ko", "/sub/sub/dir/fined.ok");

			var w = watchedFilesDb.update(fs);
			assertWatchedFiles(w, Set.of(), Set.of(), 1);

			w = watchedFilesDb.update(fs);
			assertWatchedFiles(w,
					Set.of("/sub/sub/dir/fined.ok"),
					Set.of(), 1);
		}

	}

}
