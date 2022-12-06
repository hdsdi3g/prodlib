/*
 * This file is part of transfertfiles.
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
package tv.hd3g.transfertfiles;

import static java.util.Objects.requireNonNull;
import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class CommonAbstractFileSystem<T extends AbstractFile> implements AbstractFileSystem<T> {
	private static final Logger log = LogManager.getLogger();

	private final String basePath;
	private final String standardHostName;
	protected long timeoutDuration;

	/**
	 * @param standardHostname will be use with equals/hashCode to identificate if same servers or not.
	 *        You should add user/port/server NAME (not IP - it can be non-stable), but not basePath ot protocol.
	 */
	protected CommonAbstractFileSystem(final String basePath, final String standardHostname) {
		this.basePath = normalizePath(requireNonNull(basePath, "basePath"));
		this.standardHostName = getClass().getSimpleName()
								+ "#"
								+ requireNonNull(standardHostname, "\"standardHostname\" can't to be null")
								+ basePath;
		log.trace("Init FileSystem, basePath={} standardHostname={} hashCode=#{}",
				basePath, standardHostName, hashCode());
		timeoutDuration = 0;
	}

	/**
	 * DO NOT LEAK TO USER!
	 * @return absolute path from server.
	 */
	public String getPathFromRelative(final String path) {
		return normalizePath(basePath + normalizePath(path));
	}

	public String getBasePath() {
		return basePath;
	}

	/**
	 * Use standardHostName to alter hashCode/equals behavior.
	 */
	@Override
	public final int hashCode() {
		return Objects.hash(standardHostName, basePath);
	}

	/**
	 * Use standardHostName to alter hashCode/equals behavior.
	 */
	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (CommonAbstractFileSystem<?>) obj;
		return hashCode() == other.hashCode();
	}

	@Override
	public void setTimeout(final long duration, final TimeUnit unit) {
		if (duration > 0) {
			timeoutDuration = unit.toMillis(duration);
		} else {
			throw new IllegalArgumentException("Can't set a timeoutDuration to " + timeoutDuration);
		}
		if (timeoutDuration > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can't set a timeoutDuration > Integer.MAX_VALUE: "
											   + timeoutDuration);
		}
	}

	public long getTimeout() {
		return timeoutDuration;
	}
}
