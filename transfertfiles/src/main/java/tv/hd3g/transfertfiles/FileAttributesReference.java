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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.transfertfiles;

import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

public class FileAttributesReference {

	protected final String path;
	protected final long length;
	protected final long lastModified;
	protected final boolean exists;
	protected final boolean directory;
	protected final boolean file;
	protected final boolean link;
	protected final boolean special;

	public FileAttributesReference(final String path,
								   final long length,
								   final long lastModified,
								   final boolean exists,
								   final boolean directory,
								   final boolean file,
								   final boolean link,
								   final boolean special) {
		this.path = path;
		this.length = length;
		this.lastModified = lastModified;
		this.exists = exists;
		this.directory = directory;
		this.file = file;
		this.link = link;
		this.special = special;
	}

	/**
	 * Create a regular file/dir
	 */
	public FileAttributesReference(final String path,
								   final long length,
								   final long lastModified,
								   final boolean exists,
								   final boolean directory) {
		this.path = path;
		this.length = length;
		this.lastModified = lastModified;
		this.directory = directory;
		this.exists = exists;
		file = directory == false;
		link = false;
		special = false;
	}

	public String getName() {
		return FilenameUtils.getName(path);
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return getPath();
	}

	public String getParentPath() {
		return FilenameUtils.getFullPathNoEndSeparator(path);
	}

	public boolean isHidden() {
		return getName().startsWith(".");
	}

	public long length() {
		return length;
	}

	public long lastModified() {
		return lastModified;
	}

	public boolean exists() {
		return exists;
	}

	public boolean isDirectory() {
		return directory;
	}

	public boolean isFile() {
		return file;
	}

	public boolean isLink() {
		return link;
	}

	public boolean isSpecial() {
		return special;
	}

	@Override
	public int hashCode() {
		return Objects.hash(directory, file, lastModified, length, link, path, special);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (FileAttributesReference) obj;
		return directory == other.directory && file == other.file && lastModified == other.lastModified
			   && length == other.length && link == other.link && Objects.equals(path, other.path)
			   && special == other.special;
	}

}
