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

import java.util.Set;

import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

public interface WatchedFilesDb {

	void setup(final ObservedFolder observedFolder, final WatchFolderPickupType pickUp);

	WatchedFiles update(final ObservedFolder observedFolder, AbstractFileSystemURL fileSystem);

	void reset(final ObservedFolder observedFolder, Set<CachedFileAttributes> foundedFiles);

}
