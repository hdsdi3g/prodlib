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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Slf4j
public class MaxSpoolQueueSizePolicy extends LimitedSpoolsBasePolicy {

	public static final Duration DEFAULT_CHECKTIME = Duration.ofHours(1);
	private static final String THE_QUEUE_SIZE_SHOULD_NOT_FILL_UP_TO = "The queue size should not fill up to ";
	private int maxSize;
	private Duration checkTime;

	@Override
	public Optional<Duration> isStatusOk(final String spoolName,
										 final WatchableSpoolJobState activeJob,
										 final Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning {
		if (maxSize < 0) {
			log.warn("You should set a valid maxSize (not {}) for this policy", maxSize);
			return Optional.empty();
		}
		if (allowSpool(spoolName) == false) {
			return Optional.empty();
		}
		if (queuedJobs.size() > maxSize) {
			throw new JobWatchdogPolicyWarning("The queue size for the spool " + spoolName
											   + " is actually up to " + queuedJobs.size()
											   + ", but this policy allow only " + maxSize + " waiting job(s).");
		}
		return Optional.ofNullable(checkTime).or(() -> Optional.ofNullable(DEFAULT_CHECKTIME));
	}

	@Override
	public void isStatusOk(final String spoolName,
						   final WatchableSpoolJobState activeJob,
						   final Set<WatchableSpoolJobState> queuedJobs,
						   final Set<WatchableBackgroundService> relativeBackgroundServices) throws JobWatchdogPolicyWarning {
		isStatusOk(spoolName, activeJob, queuedJobs);
	}

	@Override
	public String getDescription() {
		final var allow = getOnlySpools();
		if (allow.isEmpty() == false) {
			return THE_QUEUE_SIZE_SHOULD_NOT_FILL_UP_TO + maxSize + " queued jobs, only for spools " + allow;
		}

		final var deny = getNotSpools();
		if (deny.isEmpty() == false) {
			return THE_QUEUE_SIZE_SHOULD_NOT_FILL_UP_TO + maxSize + " queued jobs, but not for spools " + allow;
		}

		return THE_QUEUE_SIZE_SHOULD_NOT_FILL_UP_TO + maxSize + " queued jobs";
	}

}
