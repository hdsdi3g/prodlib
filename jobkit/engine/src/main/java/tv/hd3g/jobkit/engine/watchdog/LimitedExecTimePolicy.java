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
public class LimitedExecTimePolicy extends LimitedSpoolsBasePolicy {

	private static final String A_JOB_EXECUTION_SHOULD_NOT_LAST_LONGER_THAN = "A job execution should not last longer than ";
	private Duration maxExecTime;

	@Override
	public Optional<Duration> isStatusOk(final String spoolName,
										 final WatchableSpoolJobState activeJob,
										 final Set<WatchableSpoolJobState> queuedJobs) throws JobWatchdogPolicyWarning {
		if (maxExecTime == null
			|| maxExecTime.isNegative()
			|| maxExecTime.isZero()) {
			log.warn("You should set a maxExecTime for this policy");
			return Optional.empty();
		}
		if (allowSpool(spoolName) == false) {
			return Optional.empty();
		}

		final var oRuntime = activeJob.getRunTime();
		if (oRuntime.isEmpty()) {
			return Optional.empty();
		}
		final var runtime = oRuntime.get();
		final var remainingTimeAllowed = maxExecTime.toMillis() - runtime.toMillis();

		if (remainingTimeAllowed < 0) {
			throw new JobWatchdogPolicyWarning("This job was started " + runtime
											   + " ago, but the limit for its spool (" + spoolName + ") was "
											   + maxExecTime + " max");
		}

		return Optional.ofNullable(Duration.ofMillis(remainingTimeAllowed));
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
			return A_JOB_EXECUTION_SHOULD_NOT_LAST_LONGER_THAN + maxExecTime + ", only for spools " + allow;
		}

		final var deny = getNotSpools();
		if (deny.isEmpty() == false) {
			return A_JOB_EXECUTION_SHOULD_NOT_LAST_LONGER_THAN + maxExecTime + ", not for spools " + deny;
		}

		return A_JOB_EXECUTION_SHOULD_NOT_LAST_LONGER_THAN + maxExecTime;
	}

}
