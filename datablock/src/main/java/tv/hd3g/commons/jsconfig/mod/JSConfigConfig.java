/*
 * This file is part of jsconfig.
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
package tv.hd3g.commons.jsconfig.mod;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jsconfig")
@Data
public class JSConfigConfig {

	/**
	 * JavaScript sources files to import.
	 * Can be some files and directories, sorted by name before load.
	 */
	private List<File> src;

	private boolean allowCreateProcess;
	private boolean allowCreateThread;
	private boolean allowExperimentalOptions;
	private boolean disableHostClassLoading;
	private boolean allowIO;
	private File currentWorkingDirectory;
	private boolean allowNativeAccess;
	private boolean disableEnvironmentAccess;
	private boolean fineLevelLogger;
	private boolean disableWatchfolder;

	public List<File> getFileDirSrc() {
		return Optional.ofNullable(src).stream()
				.flatMap(List::stream)
				.distinct()
				.map(f -> {
					if (f.exists() == false) {
						throw new UncheckedIOException(new FileNotFoundException("File/dir don't exists: " + f));
					}
					try {
						return f.getCanonicalFile();
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't access to file/dir", e);
					}
				})
				.toList();
	}

	public Set<File> getDirSrc() {
		return getFileDirSrc().stream()
				.filter(File::isDirectory)
				.collect(toUnmodifiableSet());
	}

	public Set<File> getFileSrc() {
		return getFileDirSrc().stream()
				.filter(not(File::isDirectory))
				.collect(toUnmodifiableSet());
	}

}
