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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_DIRS;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

/**
 * Not thread safe
 */
public class WatchedFilesInMemoryDb implements WatchedFilesDb {
	private static final Logger log = LogManager.getLogger();

	private final Map<CachedFileAttributes, FileInMemoryDb> allWatchedFiles;

	private final int defaultMaxDeep;
	private WatchFolderPickupType pickUp;
	private Duration minFixedStateTime;
	private WatchedFileScanner scanner;

	public WatchedFilesInMemoryDb(final int defaultMaxDeep) {
		this.defaultMaxDeep = defaultMaxDeep;
		allWatchedFiles = new HashMap<>();
	}

	public WatchedFilesInMemoryDb() {
		this(10);
	}

	@Override
	public void setup(final ObservedFolder observedFolder, final WatchFolderPickupType pickUp) {
		if (observedFolder.isDisabled()) {
			throw new IllegalArgumentException("Can't setup a disabled observedFolder: " + observedFolder);
		}
		scanner = new WatchedFileScanner(observedFolder, defaultMaxDeep);
		this.pickUp = pickUp;
		minFixedStateTime = observedFolder.getMinFixedStateTime();
	}

	@Override
	public void reset(final ObservedFolder observedFolder, final Set<CachedFileAttributes> foundedFiles) {
		foundedFiles.forEach(allWatchedFiles::remove);
	}

	@Override
	public WatchedFiles update(final ObservedFolder observedFolder, final AbstractFileSystemURL fileSystem) {
		final var detected = scanner.scan(fileSystem);

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

}
