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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;

@Getter
@Setter
public class ObservedFolder {

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String targetFolder;
	private String label;
	private Set<String> allowedExtentions;
	private Set<String> blockedExtentions;
	private Set<String> ignoreRelativePaths;
	private Set<String> ignoreFiles;
	private boolean allowedHidden;
	private boolean allowedLinks;
	private boolean recursive;
	private Duration minFixedStateTime;
	private boolean disabled;

	private static final UnaryOperator<String> removeFirstDot = ext -> {
		if (ext.startsWith(".")) {
			return ext.substring(1);
		}
		return ext;
	};

	void postConfiguration() {
		if (disabled) {
			return;
		}
		Objects.requireNonNull(targetFolder, "Null targetFolder");
		try {
			final var targetFolderF = new File(targetFolder);
			AbstractFileSystemURL checkParse;
			if (targetFolderF.exists()) {
				final var newTargetFolder = "file://localhost" + normalizePath(targetFolderF.getCanonicalFile()
						.getAbsolutePath());
				checkParse = new AbstractFileSystemURL(newTargetFolder);
				targetFolder = newTargetFolder;
			} else {
				checkParse = new AbstractFileSystemURL(targetFolder);
			}
			checkParse.close();

			if (label == null || label.isEmpty()) {
				label = checkParse.toString();
			}
		} catch (final IOException e1) {
			throw new UncheckedIOException(new IOException("Can't load: \"" + targetFolder + "\""));
		}

		allowedExtentions = Optional.ofNullable(allowedExtentions).orElse(Set.of()).stream()
				.map(removeFirstDot)
				.map(String::toLowerCase)
				.distinct()
				.collect(toUnmodifiableSet());
		blockedExtentions = Optional.ofNullable(blockedExtentions).orElse(Set.of()).stream()
				.map(removeFirstDot)
				.map(String::toLowerCase)
				.distinct()
				.collect(toUnmodifiableSet());
		ignoreRelativePaths = Optional.ofNullable(ignoreRelativePaths).orElse(Set.of()).stream()
				.map(path -> path.replace('\\', '/'))
				.map(AbstractFile::normalizePath)
				.distinct()
				.collect(toUnmodifiableSet());

		ignoreFiles = Optional.ofNullable(ignoreFiles).orElse(Set.of()).stream()
				.map(String::toLowerCase)
				.distinct()
				.collect(toUnmodifiableSet());
		minFixedStateTime = Optional.ofNullable(minFixedStateTime).orElse(Duration.ZERO);
	}

	@Override
	public String toString() {
		return label;
	}

	public AbstractFileSystemURL createFileSystem() {
		return new AbstractFileSystemURL(targetFolder);
	}

}
