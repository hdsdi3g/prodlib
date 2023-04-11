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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

/**
 * @param founded file/dir added on scanned folder
 * @param losted file/dir removed before validation on scanned folder
 * @param updated file/dir updated (date/size change) after be founded
 * @param totalFiles total file/dir count on scanned folder (valided)
 */
public record WatchedFiles(Set<CachedFileAttributes> founded,
						   Set<? extends FileAttributesReference> losted,
						   Set<CachedFileAttributes> updated,
						   int totalFiles) {

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("WatchedFiles [founded=");
		builder.append(founded.stream().map(CachedFileAttributes::getPath).collect(joining(", ")));
		builder.append(", losted=");
		builder.append(losted.stream().map(FileAttributesReference::getPath).collect(joining(", ")));
		builder.append(", updated=");
		builder.append(updated.stream().map(CachedFileAttributes::getPath).collect(joining(", ")));
		builder.append(", totalFiles=");
		builder.append(totalFiles);
		builder.append("]");
		return builder.toString();
	}

	@JsonIgnore
	public Set<CachedFileAttributes> foundedAndUpdated() {
		return Stream.concat(founded.stream(), updated.stream())
				.distinct()
				.collect(toUnmodifiableSet());
	}

	@JsonIgnore
	public Set<AbstractFileSystem<?>> getFoundedAndUpdatedFS() { // NOSONAR S1452
		return foundedAndUpdated().stream()
				.map(CachedFileAttributes::getAbstractFile)
				.map(AbstractFile::getFileSystem)
				.distinct()
				.collect(toUnmodifiableSet());
	}

}
