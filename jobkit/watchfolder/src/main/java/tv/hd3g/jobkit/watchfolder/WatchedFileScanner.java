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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

public class WatchedFileScanner {
	private static Logger log = LogManager.getLogger();

	@Getter
	private final int maxDeep;
	private final ObservedFolder observedFolder;
	private final Duration minFixedStateTime;

	public WatchedFileScanner(final ObservedFolder observedFolder) {
		this(observedFolder, 10);
	}

	public WatchedFileScanner(final ObservedFolder observedFolder,
							  final int maxDeep) {
		log.debug("Start internal Watchfolder configuration checks {}", observedFolder);
		this.observedFolder = Objects.requireNonNull(observedFolder);
		observedFolder.postConfiguration();
		if (observedFolder.isRecursive()) {
			this.maxDeep = maxDeep;
		} else {
			this.maxDeep = 0;
		}
		minFixedStateTime = observedFolder.getMinFixedStateTime();

		try (var fs = observedFolder.createFileSystem()) {
			/** try only to load FileSystem/configured URL */
		} catch (final IOException e) {
			throw new UncheckedIOException(new IOException("Can't load FileSystem", e));
		}
		log.debug("Setup WFDB for {}, with minFixedStateTime={} and maxDeep={}",
				observedFolder, minFixedStateTime, maxDeep);
	}

	public List<CachedFileAttributes> scan(final AbstractFileSystemURL fileSystem) {
		final var detected = new ArrayList<CachedFileAttributes>();
		log.debug("Start scan {} to {}", observedFolder, fileSystem);
		actualScan(fileSystem.getRootPath(), maxDeep, detected);
		return detected;
	}

	/**
	 * Recursive
	 */
	private void actualScan(final AbstractFile aSource,
							final int deep,
							final Collection<CachedFileAttributes> detected) {
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

	public static boolean containExtension(final String baseFileName, final Set<String> candidates) {
		return candidates.stream()
				.anyMatch(c -> baseFileName.toLowerCase().endsWith("." + c));
	}

}
