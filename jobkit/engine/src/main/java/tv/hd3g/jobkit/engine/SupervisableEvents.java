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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.engine;

import java.util.Optional;

import tv.hd3g.jobkit.engine.watchdog.JobWatchdogSpoolReport;

public interface SupervisableEvents extends SupervisableSerializer {

	default void onEnd(final Supervisable supervisable, final Optional<Exception> oError) {
	}

	default void onJobWatchdogSpoolReport(final JobWatchdogSpoolReport report) {
	}

	default void onJobWatchdogSpoolReleaseReport(final JobWatchdogSpoolReport oldReport) {
	}
}
