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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.Setter;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.InvalidURLException;
import tv.hd3g.transfertfiles.URLAccess;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObservedFolder {
	private static final Logger log = LogManager.getLogger();

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String targetFolder;
	@Include
	private String label;
	private Set<String> allowedExtentions;
	private Set<String> blockedExtentions;
	private Set<String> ignoreRelativePaths;
	private Set<String> ignoreFiles;
	private Set<String> allowedFileNames;
	private Set<String> allowedDirNames;
	private Set<String> blockedFileNames;
	private Set<String> blockedDirNames;
	private boolean allowedHidden;
	private boolean allowedLinks;
	private boolean recursive;
	private Duration minFixedStateTime;
	@Include
	private boolean disabled;

	private static final UnaryOperator<String> removeFirstDot = ext -> {
		if (ext.startsWith(".")) {
			return ext.substring(1);
		}
		return ext;
	};

	private static AbstractFileSystemURL initURL(final String url) throws IOException {
		try (var fs = new AbstractFileSystemURL(url)) {
			return fs;
		}
	}

	private static AbstractFileSystemURL checkURL(final String url) throws IOException {
		AbstractFileSystemURL checkParse;
		try {
			new URLAccess(url);
		} catch (final Exception e) {
			throw new InvalidURLException("Can't found directory, or it doesn't seem to be an valid URL", url);
		}
		checkParse = initURL(url);
		return checkParse;
	}

	void postConfiguration() {
		if (disabled) {
			return;
		}

		allowedExtentions = Optional.ofNullable(allowedExtentions).orElse(Set.of());
		blockedExtentions = Optional.ofNullable(blockedExtentions).orElse(Set.of());
		ignoreRelativePaths = Optional.ofNullable(ignoreRelativePaths).orElse(Set.of());
		ignoreFiles = Optional.ofNullable(ignoreFiles).orElse(Set.of());
		minFixedStateTime = Optional.ofNullable(minFixedStateTime).orElse(Duration.ZERO);
		allowedFileNames = Optional.ofNullable(allowedFileNames).orElse(Set.of());
		allowedDirNames = Optional.ofNullable(allowedDirNames).orElse(Set.of());
		blockedFileNames = Optional.ofNullable(blockedFileNames).orElse(Set.of());
		blockedDirNames = Optional.ofNullable(blockedDirNames).orElse(Set.of());
		internalPostConfiguration();

		Objects.requireNonNull(targetFolder, "Null targetFolder");
		try {
			final var targetFolderF = new File(targetFolder);
			AbstractFileSystemURL checkParse;
			if (targetFolderF.exists()) {
				final var newTargetFolder = "file://localhost" + normalizePath(targetFolderF.getCanonicalFile()
						.getAbsolutePath());
				checkParse = initURL(newTargetFolder);
				targetFolder = newTargetFolder;
			} else {
				checkParse = checkURL(targetFolder);
			}

			if (label == null || label.isEmpty()) {
				label = checkParse.toString();
				log.warn("You should setup a label for {}", label);
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

		allowedFileNames = Optional.ofNullable(allowedFileNames).orElse(Set.of()).stream()
				.collect(toUnmodifiableSet());
		allowedDirNames = Optional.ofNullable(allowedDirNames).orElse(Set.of()).stream()
				.collect(toUnmodifiableSet());
		blockedFileNames = Optional.ofNullable(blockedFileNames).orElse(Set.of()).stream()
				.collect(toUnmodifiableSet());
		blockedDirNames = Optional.ofNullable(blockedDirNames).orElse(Set.of()).stream()
				.collect(toUnmodifiableSet());
	}

	protected void internalPostConfiguration() {
		/**
		 * Overload this as needed
		 */
	}

	@Override
	public String toString() {
		return label;
	}

	public AbstractFileSystemURL createFileSystem() {
		return new AbstractFileSystemURL(targetFolder);
	}

}
