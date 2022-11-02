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
package tv.hd3g.jobkit.watchfolder.mod.entity;

import static java.lang.Math.min;

import java.sql.Timestamp;
import java.time.Duration;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchFolderPickupType;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Entity
@Table(name = WatchedFileEntity.TABLE_NAME,
	   indexes = {
				   @Index(columnList = "hash_path", name = WatchedFileEntity.TABLE_NAME + "_hash_path_idx"),
				   @Index(columnList = "folder_label", name = WatchedFileEntity.TABLE_NAME + "_folder_label_idx")
	   })
@Getter
public class WatchedFileEntity extends BaseEntity {
	public static final String TABLE_NAME = "jobkit_wf_watchedfile";

	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(columnDefinition = "TINYINT")
	private boolean directory;

	@NotBlank
	@Column(length = 64, name = "folder_label", updatable = false)
	private String folderLabel;

	@NotBlank
	@Column(length = 4096, updatable = false)
	private String path;

	@NotBlank
	@Column(length = 32, name = "hash_path", updatable = false)
	private String hashPath;

	@Setter
	@NotNull
	@Column(name = "last_modified")
	private Timestamp lastModified;

	@Setter
	@NotNull
	@Column(name = "last_length")
	private Long lastLength;

	@Setter
	@NotNull
	@Column(name = "last_watched")
	private Timestamp lastWatched;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "marked_as_done", columnDefinition = "TINYINT")
	private boolean markedAsDone;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "last_is_same", columnDefinition = "TINYINT")
	private boolean lastIsSame;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "done_but_changed", columnDefinition = "TINYINT")
	private boolean doneButChanged;

	/**
	 * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
	 */
	public WatchedFileEntity() {
		// ONLY SET FOR HIBERNATE
	}

	public WatchedFileEntity(final CachedFileAttributes firstDetectionFile, final ObservedFolder observedFolder) {
		initCreate();
		final var label = observedFolder.getLabel();
		folderLabel = label.substring(0, min(64, label.length()));
		path = firstDetectionFile.getPath();
		hashPath = hashPath(observedFolder.getLabel(), firstDetectionFile.getPath());
		refreshNewFile(firstDetectionFile);
		directory = firstDetectionFile.isDirectory();
		lastWatched = new Timestamp(System.currentTimeMillis());
		lastIsSame = false;
		doneButChanged = false;
		markedAsDone = false;
	}

	public static final String hashPath(final String observedFolderLabel,
										final String path) {
		return DigestUtils.md5Hex(observedFolderLabel + ":" + path);// NOSONAR S4790
	}

	private void refreshNewFile(final CachedFileAttributes file) {
		lastModified = new Timestamp(file.lastModified());
		lastLength = file.length();
	}

	public WatchedFileEntity update(final CachedFileAttributes seeAgainFile) {
		if (directory) {
			if (markedAsDone == false) {
				refreshNewFile(seeAgainFile);
			}
		} else {
			lastIsSame = lastModified.getTime() == seeAgainFile.lastModified()
						 && lastLength == seeAgainFile.length();
			if (lastIsSame == false) {
				lastWatched = new Timestamp(System.currentTimeMillis());
				if (markedAsDone) {
					doneButChanged = true;
				}
			}
			refreshNewFile(seeAgainFile);
		}
		return this;
	}

	public boolean isTimeQualified(final Duration minFixedStateTime) {
		final var notTooRecent = lastWatched.getTime() < System.currentTimeMillis() - minFixedStateTime.toMillis();
		return directory
			   || lastIsSame && notTooRecent;
	}

	public boolean canBePickupFromType(final WatchFolderPickupType pickUp) {
		return directory && pickUp.isPickUpDirs() || directory == false && pickUp.isPickUpFiles();
	}

	public WatchedFileEntity resetDoneButChanged() {
		doneButChanged = false;
		return this;
	}

	public WatchedFileEntity setMarkedAsDone() {
		markedAsDone = true;
		return this;
	}

	public FileAttributesReference toFileAttributesReference(final boolean exists) {
		return new FileAttributesReference(path, lastLength, lastModified.getTime(), exists, directory);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("WatchedFileEntity [directory=");
		builder.append(directory);
		builder.append(", folderLabel=");
		builder.append(folderLabel);
		builder.append(", path=");
		builder.append(path);
		builder.append(", hashPath=");
		builder.append(hashPath);
		builder.append(", lastModified=");
		builder.append(lastModified);
		builder.append(", lastLength=");
		builder.append(lastLength);
		builder.append(", lastWatched=");
		builder.append(lastWatched);
		builder.append(", markedAsDone=");
		builder.append(markedAsDone);
		builder.append(", lastIsSame=");
		builder.append(lastIsSame);
		builder.append(", doneButChanged=");
		builder.append(doneButChanged);
		builder.append("]");
		return builder.toString();
	}

}
