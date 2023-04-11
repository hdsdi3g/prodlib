/*
 * This file is part of AuthKit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.jobkit.watchfolder.mod.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import tv.hd3g.jobkit.watchfolder.mod.entity.WatchedFileEntity;

public interface WatchedFileEntityRepository extends JpaRepository<WatchedFileEntity, Long> {

	@Query("SELECT w FROM WatchedFileEntity w WHERE w.hashPath IN :hashPath")
	Set<WatchedFileEntity> getByHashPath(Set<String> hashPath);

	@Query("""
			SELECT w FROM WatchedFileEntity w
			WHERE w.markedAsDone = 0
			AND ((w.directory = 1 AND :pickUpDirs IS true) OR (w.directory = 0 AND :pickUpFiles IS true))
			AND w.hashPath NOT IN :detectedHashPath
			AND w.folderLabel = :folderLabel
			""")
	Set<WatchedFileEntity> getLostedByHashPath(Set<String> detectedHashPath,
											   boolean pickUpDirs,
											   boolean pickUpFiles,
											   String folderLabel);

	@Query("""
			SELECT w FROM WatchedFileEntity w
			WHERE w.markedAsDone = 0
			AND ((w.directory = 1 AND :pickUpDirs IS true) OR (w.directory = 0 AND :pickUpFiles IS true))
			AND w.folderLabel = :folderLabel
			""")
	Set<WatchedFileEntity> getLostedForEmptyDir(boolean pickUpDirs,
												boolean pickUpFiles,
												String folderLabel);

	@Query("SELECT w.hashPath FROM WatchedFileEntity w WHERE w.folderLabel = :folderLabel")
	Set<String> getAllHashPathByFolderLabel(String folderLabel);

	@Query("DELETE FROM WatchedFileEntity WHERE hashPath IN :detectedHashPath")
	@Modifying
	void deleteByHashPath(Set<String> detectedHashPath);

	@Query("SELECT COUNT(w) FROM WatchedFileEntity w WHERE w.folderLabel = :folderLabel")
	int countByFolderLabel(String folderLabel);

	@Query("""
			SELECT COUNT(w) FROM WatchedFileEntity w
			WHERE ((w.directory = 1 AND :pickUpDirs IS true) OR (w.directory = 0 AND :pickUpFiles IS true))
			AND w.folderLabel = :folderLabel
			""")
	int countByFolderLabel(boolean pickUpDirs, boolean pickUpFiles, String folderLabel);

	@Query("DELETE FROM WatchedFileEntity WHERE folderLabel NOT IN :folderLabelToKeep")
	@Modifying
	void deleteFolderLabelsNotInSet(Set<String> folderLabelToKeep);

}
