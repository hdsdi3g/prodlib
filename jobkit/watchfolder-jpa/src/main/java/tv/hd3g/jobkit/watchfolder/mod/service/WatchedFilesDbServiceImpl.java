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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_DIRS;
import static tv.hd3g.jobkit.watchfolder.mod.entity.WatchedFileEntity.hashPath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchFolderPickupType;
import tv.hd3g.jobkit.watchfolder.WatchedFileSetupManager;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.mod.component.WatchedFileScannerSupplier;
import tv.hd3g.jobkit.watchfolder.mod.entity.WatchedFileEntity;
import tv.hd3g.jobkit.watchfolder.mod.repository.WatchedFileEntityRepository;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Service
public class WatchedFilesDbServiceImpl implements WatchedFilesDbService {
	private static Logger log = LogManager.getLogger();

	@Autowired
	private WatchedFileSetupManager watchedFileSetupManager;
	@Autowired
	private WatchedFileEntityRepository watchedFileEntityRepository;
	@Autowired
	private WatchedFileScannerSupplier watchedFileScannerSupplier;

	@Override
	public void setup(final ObservedFolder observedFolder, final WatchFolderPickupType pickUp) {
		watchedFileSetupManager.put(observedFolder, watchedFileScannerSupplier.create(observedFolder), pickUp);
	}

	@Override
	@Transactional
	public void purgeOlderWF(final Collection<ObservedFolder> currentObservedFolders) {
		if (currentObservedFolders.isEmpty()) {
			return;
		}
		final var folderLabelToKeep = currentObservedFolders.stream()
				.map(ObservedFolder::getLabel)
				.collect(toUnmodifiableSet());
		watchedFileEntityRepository.deleteFolderLabelsNotInSet(folderLabelToKeep);
	}

	@Override
	@Transactional
	public WatchedFiles update(final ObservedFolder observedFolder, final AbstractFileSystemURL fileSystem) {
		final var folderLabel = observedFolder.getLabel();
		final var minFixedStateTime = observedFolder.getMinFixedStateTime();
		final var watchedFileScannerProviderEntry = watchedFileSetupManager.get(observedFolder);
		final var allWatchedFilesHashPath = watchedFileEntityRepository
				.getAllHashPathByFolderLabel(folderLabel);
		final var pickUp = watchedFileScannerProviderEntry.pickUp();
		final var detected = watchedFileScannerProviderEntry.scanner().scan(fileSystem);

		/**
		 * Update all founded
		 */
		final var detectedByhashKey = detected.stream()
				.collect(toUnmodifiableMap(
						d -> hashPath(folderLabel,
								d.getPath()), d -> d));
		log.trace("detectedByhashKey={}", detectedByhashKey);

		Map<String, WatchedFileEntity> entitiesByHashKey;
		if (detectedByhashKey.isEmpty() == false) {
			entitiesByHashKey = watchedFileEntityRepository.getByHashPath(detectedByhashKey.keySet()).stream()
					.collect(toUnmodifiableMap(WatchedFileEntity::getHashPath, wf -> wf));
		} else {
			entitiesByHashKey = new HashMap<>();
		}

		final var updateFounded = entitiesByHashKey.entrySet().stream()
				.map(entry -> entry.getValue().update(detectedByhashKey.get(entry.getKey())))
				.toList();

		/**
		 * Get updated
		 */
		final var updatedChangedFounded = updateFounded.stream()
				.filter(WatchedFileEntity::isMarkedAsDone)
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.filter(u -> u.canBePickupFromType(pickUp))
				.filter(WatchedFileEntity::isDoneButChanged)
				.map(WatchedFileEntity::resetDoneButChanged)
				.map(WatchedFileEntity::getHashPath)
				.map(detectedByhashKey::get)
				.collect(toUnmodifiableSet());

		/**
		 * Get qualified, set them marked
		 */
		final var qualifyFounded = updateFounded.stream()
				.filter(not(WatchedFileEntity::isMarkedAsDone))
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.map(WatchedFileEntity::setMarkedAsDone)
				.toList();

		/**
		 * Get only them can be callbacked
		 */
		final var qualifiedAndCallbacked = qualifyFounded.stream()
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.filter(u -> u.canBePickupFromType(pickUp))
				.map(WatchedFileEntity::getHashPath)
				.map(detectedByhashKey::get)
				.collect(toUnmodifiableSet());

		Set<FileAttributesReference> lostedAndCallbacked;
		if (detectedByhashKey.isEmpty() == false) {
			lostedAndCallbacked = watchedFileEntityRepository
					.getLostedByHashPath(
							detectedByhashKey.keySet(),
							pickUp.isPickUpDirs(),
							pickUp.isPickUpFiles(),
							folderLabel).stream()
					.map(f -> f.toFileAttributesReference(false))
					.collect(toUnmodifiableSet());
		} else {
			lostedAndCallbacked = watchedFileEntityRepository
					.getLostedForEmptyDir(
							pickUp.isPickUpDirs(),
							pickUp.isPickUpFiles(),
							folderLabel).stream()
					.map(f -> f.toFileAttributesReference(false))
					.collect(toUnmodifiableSet());
		}

		/**
		 * Add new files
		 */
		final var addNewEntites = detected.stream()
				.filter(f -> (allWatchedFilesHashPath.contains(hashPath(folderLabel, f.getPath())) == false))
				.peek(f -> log.trace("Add to Db: {} ({})", f, f.hashCode()))// NOSONAR S3864
				.map(f -> new WatchedFileEntity(f, observedFolder))
				.toList();
		if (addNewEntites.isEmpty() == false) {
			watchedFileEntityRepository.saveAll(addNewEntites);
		}

		/**
		 * Clean deleted files
		 */
		final var toClean = allWatchedFilesHashPath.stream()
				.filter(not(detectedByhashKey::containsKey))
				.collect(toUnmodifiableSet());
		if (toClean.isEmpty() == false) {
			watchedFileEntityRepository.deleteByHashPath(toClean);
		}

		log.trace(
				"Lists detected={}, updateFounded={}, updatedChangedFounded={}, qualifyFounded={}, qualifiedAndCallbacked={}, lostedAndCallbacked={}, toClean={}",
				detected,
				updateFounded,
				updatedChangedFounded,
				qualifyFounded,
				qualifiedAndCallbacked,
				lostedAndCallbacked,
				toClean);

		int size;
		if (pickUp == FILES_DIRS) {
			size = watchedFileEntityRepository.countByFolderLabel(folderLabel);
		} else {
			size = watchedFileEntityRepository.countByFolderLabel(
					pickUp.isPickUpDirs(),
					pickUp.isPickUpFiles(),
					folderLabel);
		}

		log.debug("Scan watchedFilesResult for {}: {} founded, {} lost, {} total",
				observedFolder.getLabel(),
				qualifiedAndCallbacked.size(),
				lostedAndCallbacked.size(),
				size);

		return new WatchedFiles(qualifiedAndCallbacked, lostedAndCallbacked, updatedChangedFounded, size);
	}

	@Override
	@Transactional
	public void reset(final ObservedFolder observedFolder, final Set<CachedFileAttributes> foundedFiles) {
		if (foundedFiles.isEmpty()) {
			throw new IllegalArgumentException("foundedFiles can't to be empty");
		}
		final var hashPathListToPurge = foundedFiles.stream()
				.map(ff -> hashPath(observedFolder.getLabel(), ff.getPath()))
				.collect(toUnmodifiableSet());
		watchedFileEntityRepository.deleteByHashPath(hashPathListToPurge);
	}
}
