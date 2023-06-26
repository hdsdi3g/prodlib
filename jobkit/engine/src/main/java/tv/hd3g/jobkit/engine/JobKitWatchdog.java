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
package tv.hd3g.jobkit.engine;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicy;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogPolicyWarning;
import tv.hd3g.jobkit.engine.watchdog.JobWatchdogSpoolReport;
import tv.hd3g.jobkit.engine.watchdog.WatchableBackgroundService;
import tv.hd3g.jobkit.engine.watchdog.WatchableSpoolJobState;

/**
 * ThreadSafe
 */
@Slf4j
public class JobKitWatchdog {

	private final SupervisableEvents supervisableEvents;
	private final ScheduledExecutorService scheduledExecutor;
	private final List<JobWatchdogPolicy> policies;
	private final Map<String, Set<WatchableBackgroundService>> activeServicesBySpool;
	private final Map<String, Set<WatchableSpoolJobState>> jobsBySpool;
	private final Map<String, JobWatchdogSpoolReport> tiggeredPolicyBySpool;
	private final AtomicComputeReference<ScheduledFuture<?>> nextPolicyCheck;
	private final AtomicBoolean shutdown;

	JobKitWatchdog(final SupervisableEvents supervisableEvents,
				   final ScheduledExecutorService scheduledExecutor) {
		this.supervisableEvents = supervisableEvents;
		this.scheduledExecutor = scheduledExecutor;
		policies = new ArrayList<>();
		activeServicesBySpool = new HashMap<>();
		jobsBySpool = new HashMap<>();
		tiggeredPolicyBySpool = new HashMap<>();
		nextPolicyCheck = new AtomicComputeReference<>();
		shutdown = new AtomicBoolean(false);
	}

	public JobKitWatchdog addPolicies(final JobWatchdogPolicy... policies) {
		Objects.requireNonNull(policies);
		synchronized (this.policies) {
			this.policies.addAll(Arrays.asList(policies));
		}
		return this;
	}

	/**
	 * @return an unmodifiable copy of current policies list
	 */
	public List<JobWatchdogPolicy> getPolicies() {
		synchronized (policies) {
			return policies.stream().toList();
		}
	}

	void refreshBackgroundService(final String serviceName,
								  final String spoolName,
								  final boolean enabled,
								  final long timedInterval) {
		synchronized (activeServicesBySpool) {
			activeServicesBySpool.putIfAbsent(spoolName, new HashSet<>());
			final var services = activeServicesBySpool.get(spoolName);
			services.removeIf(s -> s.serviceName().equals(spoolName));
			if (enabled) {
				services.add(
						new WatchableBackgroundService(
								serviceName,
								spoolName,
								timedInterval));
			}
		}
		scheduledExecutor.execute(new Policies());
	}

	private void addWatchableJob(final Set<WatchableSpoolJobState> jobs,
								 final WatchableSpoolJob job,
								 final Date createdDate,
								 final long statedDate) {
		Optional<Long> oStartedDate = Optional.empty();
		if (statedDate > 0l) {
			oStartedDate = Optional.ofNullable(statedDate);
		}
		jobs.add(
				new WatchableSpoolJobState(createdDate,
						job.getCommandName(),
						job.getCreatedIndex(),
						job.getCreator(),
						oStartedDate));
	}

	void addJob(final WatchableSpoolJob job) {
		final var createdDate = new Date();
		synchronized (jobsBySpool) {
			jobsBySpool.putIfAbsent(job.getSpoolName(), new HashSet<>());
			final var jobs = jobsBySpool.get(job.getSpoolName());
			addWatchableJob(jobs, job, createdDate, 0);
		}
		scheduledExecutor.execute(new Policies());
	}

	private WatchableSpoolJobState getOldAndRemoveJobInSpool(final WatchableSpoolJob job,
															 final Set<WatchableSpoolJobState> jobs) {
		final var createdIndex = job.getCreatedIndex();
		final var createdJob = jobs.stream()
				.filter(j -> j.createdIndex() == createdIndex)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Can't found job {}/{} #{} in current active jobs"));
		jobs.remove(createdJob);
		return createdJob;
	}

	void startJob(final WatchableSpoolJob job, final long startedDate) {
		synchronized (jobsBySpool) {
			final var jobs = jobsBySpool.get(job.getSpoolName());
			final var createdJob = getOldAndRemoveJobInSpool(job, jobs);
			addWatchableJob(jobs, job, createdJob.createdDate(), startedDate);
		}
		scheduledExecutor.execute(new Policies());
	}

	void endJob(final WatchableSpoolJob job) {
		synchronized (jobsBySpool) {
			final var jobs = jobsBySpool.get(job.getSpoolName());
			getOldAndRemoveJobInSpool(job, jobs);
		}
		scheduledExecutor.execute(new Policies());
	}

	private static <T> Map<String, Set<T>> deepCloneFilterEmpty(final Map<String, Set<T>> map) {
		return map.keySet().stream()
				.filter(key -> map.get(key).isEmpty() == false)
				.collect(toUnmodifiableMap(key -> key,
						key -> map.get(key)
								.stream()
								.collect(toUnmodifiableSet())));
	}

	private class Policies implements Runnable {

		private Map<String, Set<WatchableSpoolJobState>> currentJobsBySpool;
		private Map<String, Set<WatchableBackgroundService>> currentServicesBySpool;

		private Set<WatchableSpoolJobState> getQueuedJobs(final String spoolName) {
			return currentJobsBySpool.get(spoolName)
					.stream()
					.filter(j -> j.startedDate().isPresent() == false)
					.collect(toUnmodifiableSet());
		}

		private Optional<WatchableSpoolJobState> getActiveJob(final String spoolName) {
			return currentJobsBySpool.get(spoolName)
					.stream()
					.filter(j -> j.startedDate().isPresent())
					.findFirst();
		}

		private void tiggerPolicy(final String spoolName,
								  final WatchableSpoolJobState activeJob,
								  final Set<WatchableSpoolJobState> queuedJobs,
								  final Set<WatchableBackgroundService> relativeBackgroundServices,
								  final JobWatchdogPolicy policy,
								  final JobWatchdogPolicyWarning warning) {
			synchronized (tiggeredPolicyBySpool) {
				if (tiggeredPolicyBySpool.containsKey(spoolName)) {
					log.trace("Policy \"{}\" was rise a warn, again, on {}", policy.getDescription(), spoolName);
					return;
				}
				final var report = new JobWatchdogSpoolReport(
						new Date(),
						spoolName,
						activeJob,
						queuedJobs,
						policy,
						warning,
						relativeBackgroundServices);
				tiggeredPolicyBySpool.put(spoolName, report);

				log.warn("Policy \"{}\" rise a warn on {}: {}", policy.getDescription(), spoolName);
				log.debug("Send report: {}", report);
				supervisableEvents.onJobWatchdogSpoolReport(report);
			}
		}

		private void releasePolicy(final String spoolName, final JobWatchdogPolicy policy) {
			synchronized (tiggeredPolicyBySpool) {
				final var oldReport = tiggeredPolicyBySpool.get(spoolName);
				if (oldReport == null || oldReport.policy().equals(policy) == false) {
					return;
				}
				tiggeredPolicyBySpool.remove(spoolName);

				log.info("Policy \"{}\" release a warn on {}: {}", policy.getDescription(), spoolName);
				log.debug("Send report: {}", oldReport);
				supervisableEvents.onJobWatchdogSpoolReleaseReport(oldReport);
			}
		}

		private long applyPoliciesAndGetLowerNextTimeToCheckOnRegularSpools(final Set<String> regularSpools,
																			final JobWatchdogPolicy policy,
																			final String description) {
			log.debug("Apply policy: {}, on regular spools: {}", description, regularSpools);
			return regularSpools.stream().mapToLong(spoolName -> {
				final var oActiveJob = getActiveJob(spoolName);
				if (oActiveJob.isEmpty()) {
					return MAX_VALUE;
				}
				final var activeJob = oActiveJob.get();
				final var queuedJobs = getQueuedJobs(spoolName);
				try {
					final var durationToQueue = policy.isStatusOk(
							spoolName,
							activeJob,
							queuedJobs)
							.map(Duration::toMillis)
							.orElse(0l);
					releasePolicy(spoolName, policy);
					if (durationToQueue > 0) {
						return durationToQueue;
					}
				} catch (final JobWatchdogPolicyWarning e) {
					tiggerPolicy(spoolName, activeJob, queuedJobs, Set.of(), policy, e);
				}
				return MAX_VALUE;
			}).min().orElse(MAX_VALUE);
		}

		private long applyPoliciesAndGetLowerNextTimeToCheckOnServicesSpools(final Set<String> serviceSpools,
																			 final JobWatchdogPolicy policy,
																			 final String description) {
			log.debug("Apply policy: {}, on services spools: {}", description, serviceSpools);
			return serviceSpools.stream().mapToLong(spoolName -> {
				final var oActiveJob = getActiveJob(spoolName);
				if (oActiveJob.isEmpty()) {
					return MAX_VALUE;
				}
				final var activeJob = oActiveJob.get();
				final var queuedJobs = getQueuedJobs(spoolName);
				final var relativeBackgroundServices = currentServicesBySpool.get(spoolName)
						.stream()
						.collect(toUnmodifiableSet());
				try {
					policy.isStatusOk(
							spoolName,
							oActiveJob.get(),
							queuedJobs,
							relativeBackgroundServices);

					final var nextInterval = currentServicesBySpool.get(spoolName).stream()
							.mapToLong(WatchableBackgroundService::timedInterval)
							.min()
							.orElse(MAX_VALUE);
					releasePolicy(spoolName, policy);
					return nextInterval;
				} catch (final JobWatchdogPolicyWarning e) {
					tiggerPolicy(spoolName, activeJob, queuedJobs, relativeBackgroundServices, policy, e);
				}
				return MAX_VALUE;
			}).min().orElse(MAX_VALUE);
		}

		@Override
		public void run() {
			if (shutdown.get()) {
				log.debug("Don't apply policies: shutdown");
				return;
			}
			synchronized (activeServicesBySpool) {
				currentServicesBySpool = deepCloneFilterEmpty(activeServicesBySpool);
			}
			synchronized (jobsBySpool) {
				currentJobsBySpool = deepCloneFilterEmpty(jobsBySpool);
			}

			final var regularSpools = currentJobsBySpool.keySet().stream()
					.filter(not(currentServicesBySpool::containsKey))
					.collect(toUnmodifiableSet());
			final var serviceSpools = currentJobsBySpool.keySet().stream()
					.filter(currentServicesBySpool::containsKey)
					.collect(toUnmodifiableSet());

			final var oLowerDurationToQueue = getPolicies().stream()
					.mapToLong(policy -> {
						final var description = policy.getDescription();

						final var lowerDurationToQueueRegular = applyPoliciesAndGetLowerNextTimeToCheckOnRegularSpools(
								regularSpools, policy, description);
						final var lowerDurationToQueueService = applyPoliciesAndGetLowerNextTimeToCheckOnServicesSpools(
								serviceSpools, policy, description);

						log.trace("Next lowerDurationToQueueRegular={}, lowerDurationToQueueService={}",
								Duration.ofMillis(lowerDurationToQueueRegular),
								Duration.ofMillis(lowerDurationToQueueService));

						return Math.min(lowerDurationToQueueRegular, lowerDurationToQueueService);
					})
					.min();

			if (oLowerDurationToQueue.isEmpty()
				|| oLowerDurationToQueue.getAsLong() == MAX_VALUE) {
				return;
			}

			final var lowerDurationToQueue = oLowerDurationToQueue.getAsLong();

			log.debug("Next lowerDurationToQueue={}", Duration.ofMillis(lowerDurationToQueue));

			nextPolicyCheck.replace(actualSch -> {
				if (actualSch != null
					&& actualSch.isDone() == false
					&& actualSch.isCancelled() == false) {
					if (actualSch.getDelay(MILLISECONDS) < lowerDurationToQueue) {
						log.trace("Don't need to remove previous scheduled: {} ms instead of {} ms",
								Duration.ofMillis(actualSch.getDelay(MILLISECONDS)),
								Duration.ofMillis(lowerDurationToQueue));
						return actualSch;
					}

					log.trace("Cancel previous scheduled {}", actualSch);
					actualSch.cancel(false);
				}
				log.trace("Scheduled next {} in {}", actualSch, Duration.ofMillis(lowerDurationToQueue));
				return scheduledExecutor.schedule(new Policies(), lowerDurationToQueue, MILLISECONDS);
			});

		}

	}

	void shutdown() {
		shutdown.set(true);
		log.debug("Close JobKitWatchDog");
		Optional.ofNullable(nextPolicyCheck.reset()).ifPresent(n -> n.cancel(true));
	}

}
