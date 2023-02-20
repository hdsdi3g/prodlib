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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.ftp.FTPFileSystem;

class ObservedFolderTest {

	String targetFolder;
	String label;
	Set<String> allowedExtentions;
	Set<String> blockedExtentions;
	Set<String> ignoreRelativePaths;
	Set<String> ignoreFiles;
	Duration minFixedStateTime;

	ObservedFolder observedFolder;

	@BeforeEach
	void init() {
		targetFolder = new File("").getAbsolutePath();
		label = "lbl-" + String.valueOf(Math.abs(System.nanoTime()));
		allowedExtentions = Set.of(".ok", "gO", ".a.b", "c.d", "e.f.g");
		blockedExtentions = Set.of("no", ".nEver", ".Na.Nb", "Nc.Nd", "Ne.Nf.Ng");
		ignoreRelativePaths = Set.of("/never/here", "nope\\dir");
		ignoreFiles = Set.of("desktop.ini", ".DS_Store");
		minFixedStateTime = Duration.ofSeconds(1);

		observedFolder = new ObservedFolder();
	}

	@Test
	void testToString() {
		observedFolder.setLabel(label);
		assertEquals(label, observedFolder.toString());
	}

	@Test
	void testPostConfiguration() {
		observedFolder.setTargetFolder(targetFolder);
		observedFolder.setAllowedExtentions(allowedExtentions);
		observedFolder.setBlockedExtentions(blockedExtentions);
		observedFolder.setIgnoreFiles(ignoreFiles);
		observedFolder.setIgnoreRelativePaths(ignoreRelativePaths);
		observedFolder.setLabel(label);
		observedFolder.postConfiguration();

		assertEquals(Set.of("ok", "go", "a.b", "c.d", "e.f.g"), observedFolder.getAllowedExtentions());
		assertEquals(Set.of("no", "never", "na.nb", "nc.nd", "ne.nf.ng"), observedFolder.getBlockedExtentions());
		assertEquals(Set.of("/never/here", "/nope/dir"), observedFolder.getIgnoreRelativePaths());
		assertEquals(Set.of("desktop.ini", ".ds_store"), observedFolder.getIgnoreFiles());
	}

	@Test
	void testPostConfiguration_disabled() {
		observedFolder.setTargetFolder(new File("/" + String.valueOf(System.nanoTime())).getPath());
		observedFolder.setDisabled(true);
		assertDoesNotThrow(() -> observedFolder.postConfiguration());
	}

	@Test
	void testPostConfiguration_errors() {
		assertThrows(NullPointerException.class, () -> observedFolder.postConfiguration());
		observedFolder.setTargetFolder(new File("/" + String.valueOf(System.nanoTime())).getPath());
		assertThrows(NullPointerException.class, () -> observedFolder.postConfiguration());
		observedFolder.setTargetFolder(new File("pom.xml").getPath());
		assertThrows(UncheckedIOException.class, () -> observedFolder.postConfiguration());
	}

	@Test
	void testPostConfiguration_minimal() {
		observedFolder.setTargetFolder(targetFolder);
		observedFolder.postConfiguration();
		assertNotNull(observedFolder.getLabel());
		assertFalse(observedFolder.getLabel().isEmpty());

		assertEquals(Set.of(), observedFolder.getAllowedExtentions());
		assertEquals(Set.of(), observedFolder.getBlockedExtentions());
		assertEquals(Set.of(), observedFolder.getIgnoreRelativePaths());
		assertEquals(Set.of(), observedFolder.getIgnoreFiles());
		assertEquals(Duration.ZERO, observedFolder.getMinFixedStateTime());
	}

	@Test
	void testPostConfiguration_URL() {
		observedFolder.setTargetFolder("ftp://user@localhost/");
		observedFolder.postConfiguration();
		assertTrue(observedFolder.createFileSystem().getFileSystem() instanceof FTPFileSystem);
	}

}
