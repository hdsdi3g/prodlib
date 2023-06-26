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
public class LimitedServiceExecTimePolicy extends LimitedSpoolsBasePolicy {

	private static final String AN_INTERNAL_SERVICE_SHOULD_NOT_PRODUCE_JOBS_WHO_RUN_MORE_THAN = "An internal Service should not produce jobs who run more than ";
	private int waitFactor;

	@Override
	public Optional<Duration> isStatusOk(final String spoolName,
										 final WatchableSpoolJobState activeJob,
										 final Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning {
		return Optional.empty();
	}

	@Override
	public void isStatusOk(final String spoolName,
						   final WatchableSpoolJobState activeJob,
						   final Set<WatchableSpoolJobState> queuedJobs,
						   final Set<WatchableBackgroundService> relativeBackgroundServices) throws JobWatchdogPolicyWarning {
		if (waitFactor < 1) {
			log.warn("You should set a valid waitFactor (not {}) for this policy", waitFactor);
			return;
		}
		if (allowSpool(spoolName) == false) {
			return;
		}
		final var oRuntime = activeJob.getRunTime();
		if (oRuntime.isEmpty()) {
			return;
		}
		final var runtime = oRuntime.get();
		final var maxExecTime = relativeBackgroundServices.stream()
				.mapToLong(WatchableBackgroundService::timedInterval)
				.max()
				.orElseThrow();

		final var remainingTimeAllowed = maxExecTime * waitFactor - runtime.toMillis();

		if (remainingTimeAllowed < 0) {
			throw new JobWatchdogPolicyWarning("This job was started " + runtime
											   + " ago, but max limit time for all the Services run on this spool ("
											   + spoolName + ") was "
											   + Duration.ofMillis(maxExecTime * waitFactor) + " max");
		}
	}

	@Override
	public String getDescription() {
		final var allow = getOnlySpools();
		if (allow.isEmpty() == false) {
			return AN_INTERNAL_SERVICE_SHOULD_NOT_PRODUCE_JOBS_WHO_RUN_MORE_THAN + waitFactor
				   + " times the time between two Service runs, only for spools " + allow;
		}

		final var deny = getNotSpools();
		if (deny.isEmpty() == false) {
			return AN_INTERNAL_SERVICE_SHOULD_NOT_PRODUCE_JOBS_WHO_RUN_MORE_THAN + waitFactor
				   + " times the time between two Service runs, not for spools " + allow;
		}

		return AN_INTERNAL_SERVICE_SHOULD_NOT_PRODUCE_JOBS_WHO_RUN_MORE_THAN + waitFactor
			   + " times the time between two Service runs";
	}

}
