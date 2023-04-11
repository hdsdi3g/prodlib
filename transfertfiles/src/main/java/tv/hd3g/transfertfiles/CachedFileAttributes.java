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

import static tv.hd3g.transfertfiles.json.TransfertFilesSerializer.DEFAULT_HASHCODE;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import tv.hd3g.transfertfiles.json.TransfertFilesSerializer.CachedFileAttributeSerializer;
import tv.hd3g.transfertfiles.json.TransfertFilesSerializer.CachedFileAttributesDeserializer;

@JsonSerialize(using = CachedFileAttributeSerializer.class)
@JsonDeserialize(using = CachedFileAttributesDeserializer.class)
public class CachedFileAttributes extends FileAttributesReference {

	private final AbstractFile abstractFile;
	private final int hashCode;

	/**
	 * Attributes and AbstractFile attributes shoud be the same values (at the least on the creation).
	 */
	public CachedFileAttributes(final AbstractFile abstractFile,
								final long length,
								final long lastModified,
								final boolean exists,
								final boolean directory,
								final boolean file,
								final boolean link,
								final boolean special) {
		super(abstractFile.getPath(), length, lastModified, exists, directory, file, link, special);
		this.abstractFile = Objects.requireNonNull(abstractFile);
		hashCode = abstractFile.hashCode();
	}

	/**
	 * Not optimized approach directly derived from AbstractFile.
	 */
	public CachedFileAttributes(final AbstractFile abstractFile) {
		super(abstractFile.getPath(),
				abstractFile.length(),
				abstractFile.lastModified(),
				abstractFile.exists(),
				abstractFile.isDirectory(),
				abstractFile.isFile(),
				abstractFile.isLink(),
				abstractFile.isSpecial());
		this.abstractFile = Objects.requireNonNull(abstractFile);
		hashCode = abstractFile.hashCode();
	}

	public static CachedFileAttributes notExists(final AbstractFile abstractFile) {
		return new CachedFileAttributes(abstractFile, 0, 0, false, false, false, false, false);
	}

	/**
	 * @return the original data source. Warning: linked AbstractFileSystem may be disconnected.
	 */
	public AbstractFile getAbstractFile() {
		return abstractFile;
	}

	/**
	 * Take abstractFile.hashCode();
	 */
	@Override
	public int hashCode() {
		if (hashCode == DEFAULT_HASHCODE) {
			throw new IllegalStateException("You can't use hashCode with a disconnected item.");
		}
		return hashCode;
	}

	/**
	 * Take abstractFile.hashCode();
	 */
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
		final var other = (CachedFileAttributes) obj;
		if (hashCode == DEFAULT_HASHCODE || other.hashCode == DEFAULT_HASHCODE) {
			throw new IllegalStateException("You can't use equals with a disconnected item.");
		}
		return hashCode == other.hashCode;
	}

}
