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
package tv.hd3g.jobkit.watchfolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread Safe
 */
public class WatchedFileSetupManager {

	private final Map<ObservedFolder, WatchedFileScannerProviderEntry> entries;

	public WatchedFileSetupManager() {
		entries = new ConcurrentHashMap<>();
	}

	public static record WatchedFileScannerProviderEntry(WatchedFileScanner scanner, WatchFolderPickupType pickUp) {
	}

	public void put(final ObservedFolder observedFolder,
					final WatchedFileScanner scanner,
					final WatchFolderPickupType pickUp) {
		entries.put(observedFolder, new WatchedFileScannerProviderEntry(scanner, pickUp));
	}

	public WatchedFileScannerProviderEntry get(final ObservedFolder observedFolder) {
		return entries.get(observedFolder);
	}

}
