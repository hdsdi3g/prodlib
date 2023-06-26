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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.engine.watchdog;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface JobWatchdogPolicy {

	/**
	 * @return empty/zero == no plan to future checks, else do a future check after this time.
	 */
	Optional<Duration> isStatusOk(String spoolName,
								  WatchableSpoolJobState activeJob,
								  Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning;

	void isStatusOk(String spoolName,
					WatchableSpoolJobState activeJob,
					Set<WatchableSpoolJobState> queuedJobs,
					Set<WatchableBackgroundService> relativeBackgroundServices) throws JobWatchdogPolicyWarning;

	String getDescription();

}
