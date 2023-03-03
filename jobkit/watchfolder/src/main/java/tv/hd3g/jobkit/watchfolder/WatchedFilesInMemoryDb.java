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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_DIRS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

/**
 * Not thread safe
 */
public class WatchedFilesInMemoryDb implements WatchedFilesDb {
	private static final Logger log = LogManager.getLogger();

	private final Map<CachedFileAttributes, FileInMemoryDb> allWatchedFiles;

	private int maxDeep;
	private ObservedFolder observedFolder;
	private WatchFolderPickupType pickUp;
	private Duration minFixedStateTime;

	public WatchedFilesInMemoryDb() {
		allWatchedFiles = new HashMap<>();
		maxDeep = 10;
	}

	public int getMaxDeep() {
		return maxDeep;
	}

	public void setMaxDeep(final int maxDeep) {
		this.maxDeep = maxDeep;
	}

	@Override
	public void setup(final ObservedFolder observedFolder, final WatchFolderPickupType pickUp) {
		if (observedFolder.isDisabled()) {
			throw new IllegalArgumentException("Can't setup a disabled observedFolder: " + observedFolder);
		}
		this.observedFolder = observedFolder;
		observedFolder.postConfiguration();
		this.pickUp = pickUp;
		minFixedStateTime = observedFolder.getMinFixedStateTime();
		try (var fs = observedFolder.createFileSystem()) {
			/** try only to load FileSystem/configured URL */
		} catch (final IOException e) {
			throw new UncheckedIOException(new IOException("Can't load FileSystem", e));
		}
		if (observedFolder.isRecursive() == false) {
			maxDeep = 0;
		}
		log.debug("Setup WFDB for {}, pickUp: {}, minFixedStateTime: {}, maxDeep: {}",
				observedFolder.getLabel(), pickUp, minFixedStateTime, maxDeep);
	}

	@Override
	public void reset(final Set<CachedFileAttributes> foundedFiles) {
		foundedFiles.forEach(allWatchedFiles::remove);
	}

	@Override
	public WatchedFiles update(final AbstractFileSystemURL fileSystem) {
		final var detected = new HashSet<CachedFileAttributes>();
		actualScan(fileSystem.getRootPath(), maxDeep, detected);

		/**
		 * update all founded
		 */
		final var updateFounded = detected.stream()
				.filter(allWatchedFiles::containsKey)
				.map(f -> allWatchedFiles.get(f).update(f))
				.toList();

		/**
		 * get updated
		 */
		final var updatedChangedFounded = updateFounded.stream()
				.filter(not(FileInMemoryDb::isNotYetMarkedAsDone))// == MarkedAsDone
				.filter(FileInMemoryDb::canBeCallbacked)
				.filter(FileInMemoryDb::isDoneButChanged)
				.map(FileInMemoryDb::resetDoneButChanged)
				.map(FileInMemoryDb::getLastFile)
				.collect(toUnmodifiableSet());

		/**
		 * get qualified, set them marked
		 */
		final var qualifyFounded = updateFounded.stream()
				.filter(FileInMemoryDb::isNotYetMarkedAsDone)
				.filter(FileInMemoryDb::isTimeQualified)
				.map(FileInMemoryDb::setMarkedAsDone)
				.toList();

		/**
		 * get only them can be callbacked
		 */
		final var qualifiedAndCallbacked = qualifyFounded.stream()
				.filter(FileInMemoryDb::canBeCallbacked)
				.map(FileInMemoryDb::getLastFile)
				.collect(toUnmodifiableSet());

		final var losted = allWatchedFiles.values().stream()
				.filter(FileInMemoryDb::isNotYetMarkedAsDone)
				.filter(w -> w.absentInSet(detected))
				.toList();

		final var lostedAndCallbacked = losted.stream()
				.filter(FileInMemoryDb::canBePickupFromType)
				.map(FileInMemoryDb::getLastFile)
				.collect(toUnmodifiableSet());

		/**
		 * Add new files
		 */
		detected.stream()
				.filter(Predicate.not(allWatchedFiles::containsKey))
				.peek(f -> log.trace("Add to Db: {} ({})", f, f.hashCode()))// NOSONAR S3864
				.forEach(f -> allWatchedFiles.put(f, new FileInMemoryDb(f, pickUp, minFixedStateTime)));

		/**
		 * Clean deleted files
		 */
		final var toClean = allWatchedFiles.keySet().stream()
				.filter(Predicate.not(detected::contains))
				.toList();
		toClean.forEach(allWatchedFiles::remove);

		log.trace(
				"Lists detected={}, updateFounded={}, updatedChangedFounded={}, qualifyFounded={}, qualifiedAndCallbacked={}, losted={}, lostedAndCallbacked={}, toClean={}",
				detected,
				updateFounded,
				updatedChangedFounded,
				qualifyFounded,
				qualifiedAndCallbacked,
				losted,
				lostedAndCallbacked,
				toClean);

		int size;
		if (pickUp == FILES_DIRS) {
			size = allWatchedFiles.size();
		} else {
			size = (int) allWatchedFiles.values().stream()
					.filter(FileInMemoryDb::canBePickupFromType)
					.count();
		}

		log.debug("Scan result for {}: {} founded, {} lost, {} total",
				observedFolder.getLabel(),
				qualifiedAndCallbacked.size(),
				lostedAndCallbacked.size(),
				size);
		return new WatchedFiles(qualifiedAndCallbacked, lostedAndCallbacked, updatedChangedFounded, size);
	}

	/**
	 * Recursive
	 */
	private void actualScan(final AbstractFile aSource,
							final int deep,
							final Set<CachedFileAttributes> detected) {
		final var ignoreFiles = observedFolder.getIgnoreFiles();
		final var allowedHidden = observedFolder.isAllowedHidden();
		final var allowedLinks = observedFolder.isAllowedLinks();
		final var allowedExtentions = observedFolder.getAllowedExtentions();
		final var blockedExtentions = observedFolder.getBlockedExtentions();
		final var ignoreRelativePaths = observedFolder.getIgnoreRelativePaths();

		final var result = aSource.toCachedList()
				.peek(f -> log.trace("Detect file={}", f))// NOSONAR S3864
				.filter(f -> ignoreFiles.contains(f.getName().toLowerCase()) == false)
				.filter(f -> (allowedHidden == false && (f.isHidden() || f.getName().startsWith("."))) == false)
				.filter(f -> (allowedLinks == false && f.isLink()) == false)
				.filter(f -> {
					if (f.isDirectory()) {
						return true;
					} else if (allowedExtentions.isEmpty() == false) {
						return containExtension(f.getName(), allowedExtentions);
					}
					return true;
				})
				.filter(f -> {
					if (f.isDirectory()) {
						return true;
					}
					return containExtension(f.getName(), blockedExtentions) == false;
				})
				.filter(f -> {
					if (ignoreRelativePaths.isEmpty()) {
						return true;
					}
					return ignoreRelativePaths.contains(f.getPath()) == false;
				})
				.filter(this::checkAllowedNotBlocked)
				.filter(f -> f.isDirectory() || f.isSpecial() == false)
				.toList();

		detected.addAll(result);

		log.debug(() -> "Scanned files/dirs for \"" + aSource.getPath() + "\" (deep " + deep + "): "
						+ result.stream()
								.map(CachedFileAttributes::getName)
								.sorted()
								.collect(joining(", "))
						+ " on \"" + aSource.getFileSystem().toString() + "\"");
		if (deep > 0) {
			result.stream()
					.filter(CachedFileAttributes::isDirectory)
					.forEach(f -> actualScan(f.getAbstractFile(), deep - 1, detected));
		}
	}

	private boolean checkAllowedNotBlocked(final CachedFileAttributes detectedFile) {
		final var name = detectedFile.getName();
		if (detectedFile.isDirectory()) {
			final var allowedDirNames = observedFolder.getAllowedDirNames();
			if (allowedDirNames.isEmpty() == false) {
				return allowedDirNames.stream().anyMatch(w -> wildcardMatch(name, w));
			}
			final var blockedDirNames = observedFolder.getBlockedDirNames();
			if (blockedDirNames.isEmpty() == false) {
				return blockedDirNames.stream().noneMatch(w -> wildcardMatch(name, w));
			}
		} else {
			final var allowedFileNames = observedFolder.getAllowedFileNames();
			if (allowedFileNames.isEmpty() == false) {
				return allowedFileNames.stream().anyMatch(w -> wildcardMatch(name, w));
			}
			final var blockedFileNames = observedFolder.getBlockedFileNames();
			if (blockedFileNames.isEmpty() == false) {
				return blockedFileNames.stream().noneMatch(w -> wildcardMatch(name, w));
			}
		}
		return true;
	}

	boolean containExtension(final String baseFileName, final Set<String> candidates) {
		return candidates.stream()
				.anyMatch(c -> baseFileName.toLowerCase().endsWith("." + c));
	}
}
