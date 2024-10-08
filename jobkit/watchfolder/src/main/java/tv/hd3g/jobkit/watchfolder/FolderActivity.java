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

import static tv.hd3g.jobkit.engine.Supervisable.getSupervisable;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;

import java.io.IOException;
import java.time.Duration;

import tv.hd3g.jobkit.engine.SupervisableEndEvent;

public interface FolderActivity {
	String OBSERVEDFOLDER = "ObservedFolder";

	void onAfterScan(final ObservedFolder observedFolder,
					 final Duration scanTime,
					 final WatchedFiles scanResult) throws IOException;

	/**
	 * On start regular scans (as service)
	 */
	default void onStartScan(final ObservedFolder observedFolder) throws IOException {
		getSupervisable()
				.setContext(OBSERVEDFOLDER, observedFolder)
				.markAsInternalStateChange()
				.resultDone("startscan", "Start scans");
	}

	/**
	 * On stop regular scans (as service)
	 */
	default void onStopScan(final ObservedFolder observedFolder) throws IOException {
		getSupervisable()
				.setContext(OBSERVEDFOLDER, observedFolder)
				.markAsInternalStateChange()
				.resultDone("stopscan", "Stop scans");
	}

	default void onBeforeScan(final ObservedFolder observedFolder) throws IOException {
	}

	default WatchFolderPickupType getPickUpType(final ObservedFolder observedFolder) {
		return WatchFolderPickupType.FILES_ONLY;
	}

	default void onScanErrorFolder(final ObservedFolder observedFolder, final Exception e) throws IOException {
		getSupervisable()
				.setContext(OBSERVEDFOLDER, observedFolder)
				.markAsInternalStateChange()
				.resultError(e);
	}

	default RetryScanPolicyOnUserError retryScanPolicyOnUserError(final ObservedFolder observedFolder,
																  final WatchedFiles scanResult,
																  final Exception e) {
		return RETRY_FOUNDED_FILE;
	}

	static boolean isFolderActivityEvent(final SupervisableEndEvent event) {
		return event.isInternalStateChangeMarked() &&
			   OBSERVEDFOLDER.equalsIgnoreCase(event.typeName());
	}
}
