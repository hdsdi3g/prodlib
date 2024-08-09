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

import static java.time.Duration.ZERO;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_ONLY;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.RunnableWithException;

@Slf4j
public class Watchfolders {
	static final int DEFAULT_RETRY_AFTER_TIME = 10;

	private final Map<ObservedFolder, WatchedFilesDb> observedFoldersDb;
	private final Map<ObservedFolder, BackgroundService> observedFoldersServices;
	private final FolderActivity folderActivity;
	private final Duration defaultTimeBetweenScans;
	private final JobKitEngine jobKitEngine;
	private final String defaultSpoolScans;
	private final String defaultSpoolEvents;

	public Watchfolders(final Collection<? extends ObservedFolder> allObservedFolders,
						final FolderActivity folderActivity,
						final Duration defaultTimeBetweenScans,
						final JobKitEngine jobKitEngine,
						final String defaultSpoolScans,
						final String defaultSpoolEvents,
						final Supplier<WatchedFilesDb> watchedFilesDbBuilder) {
		this.folderActivity = Objects.requireNonNull(folderActivity);
		this.defaultTimeBetweenScans = Objects.requireNonNull(defaultTimeBetweenScans);
		this.jobKitEngine = Objects.requireNonNull(jobKitEngine);
		this.defaultSpoolScans = Objects.requireNonNull(defaultSpoolScans);
		this.defaultSpoolEvents = Objects.requireNonNull(defaultSpoolEvents);
		Objects.requireNonNull(watchedFilesDbBuilder);

		final var allLabelsCount = (int) allObservedFolders.stream()
				.map(ObservedFolder::getLabel)
				.distinct()
				.count();
		if (allObservedFolders.size() != allLabelsCount) {
			throw new IllegalArgumentException(
					"ObservedFolders setup fail: you must have separate labels name for each entry");
		}

		final var observedFolders = Objects.requireNonNull(allObservedFolders)
				.stream()
				.filter(not(ObservedFolder::isDisabled))
				.toList();

		observedFoldersDb = observedFolders.stream()
				.collect(toUnmodifiableMap(
						of -> of,
						oF -> watchedFilesDbBuilder.get()));

		observedFoldersServices = observedFoldersDb.entrySet().stream()
				.collect(toUnmodifiableMap(
						Entry::getKey,
						entry -> initWFAndCreateService(entry.getKey(), entry.getValue())));

		if (observedFoldersDb.isEmpty()) {
			log.warn("No configured watchfolders");
		}
	}

	Map<ObservedFolder, BackgroundService> getObservedFoldersServices() {
		return observedFoldersServices;
	}

	private void justLogAfterBadUserRun(final Exception e) {
		if (e != null) {
			log.error("Can't send event", e);
		}
	}

	private RunnableWithException getServiceTask(final ObservedFolder observedFolder,
												 final WatchedFilesDb db,
												 final String name) {
		return () -> {
			try (var fs = observedFolder.createFileSystem()) {
				log.trace("Start Watchfolder scan for {} :: {}", name, fs);

				jobKitEngine.runOneShot(
						"On \"before scan\" event on watchfolder " + name,
						observedFolder.getSpoolEvents(),
						observedFolder.getJobsPriority(),
						() -> folderActivity.onBeforeScan(observedFolder), this::justLogAfterBadUserRun);

				final var startTime = System.currentTimeMillis();
				final var scanResult = db.update(observedFolder, fs);
				final var scanTime = Duration.of(System.currentTimeMillis() - startTime, MILLIS);

				jobKitEngine.runOneShot(
						"On \"after scan\" event on watchfolder " + name,
						observedFolder.getSpoolEvents(),
						observedFolder.getJobsPriority(),
						() -> folderActivity.onAfterScan(observedFolder, scanTime, scanResult),
						e -> {
							if (e == null) {
								return;
							}
							final var policy = folderActivity.retryScanPolicyOnUserError(
									observedFolder, scanResult, e);
							final var founded = scanResult.founded();
							if (founded.isEmpty() == false) {
								log.error("Can't process user event of onAfterScan ({} founded), policy is {}",
										founded.size(), policy, e);
								if (policy == RETRY_FOUNDED_FILE) {
									db.reset(observedFolder, founded);
								}
							} else {
								log.error("Can't process user event of onAfterScan", e);
							}
						});
				log.trace("Ends Watchfolder scan for {} :: {}", name, fs);
			} catch (final Exception e) {
				folderActivity.onScanErrorFolder(observedFolder, e);
				throw e;
			}
		};
	}

	BackgroundService initWFAndCreateService(final ObservedFolder observedFolder, final WatchedFilesDb db) {
		final var name = observedFolder.getLabel();

		if (observedFolder.getSpoolEvents() == null
			|| observedFolder.getSpoolEvents().equals("")) {
			observedFolder.setSpoolEvents(defaultSpoolEvents);
		}
		if (observedFolder.getSpoolScans() == null
			|| observedFolder.getSpoolScans().equals("")) {
			observedFolder.setSpoolScans(defaultSpoolScans);
		}
		if (observedFolder.getTimeBetweenScans() == null
			|| observedFolder.getTimeBetweenScans().equals(ZERO)) {
			observedFolder.setTimeBetweenScans(defaultTimeBetweenScans);
		}
		if (observedFolder.getRetryAfterTimeFactor() < 1) {
			observedFolder.setRetryAfterTimeFactor(DEFAULT_RETRY_AFTER_TIME);
		}

		final var pickUp = folderActivity.getPickUpType(observedFolder);
		db.setup(observedFolder, Optional.ofNullable(pickUp).orElse(FILES_ONLY));

		return jobKitEngine.createService(
				"Watchfolder for " + name,
				observedFolder.getSpoolScans(),
				getServiceTask(observedFolder, db, name),
				() -> folderActivity.onStopScan(observedFolder))
				.setTimedInterval(observedFolder.getTimeBetweenScans())
				.setRetryAfterTimeFactor(observedFolder.getRetryAfterTimeFactor())
				.setPriority(observedFolder.getJobsPriority());
	}

	public void queueManualScan() {
		observedFoldersDb.forEach((observedFolder, db) -> {
			final var name = observedFolder.getLabel();
			final var task = getServiceTask(observedFolder, db, name);

			jobKitEngine.runOneShot(
					"Start watchfolder manual scan for " + name,
					observedFolder.getSpoolScans(),
					observedFolder.getJobsPriority(),
					task,
					e -> {
					});
		});
	}

	public synchronized void startScans() {
		observedFoldersServices.forEach((oF, service) -> {
			if (service.isEnabled()) {
				return;
			}
			jobKitEngine.runOneShot(
					"Start (enable) watchfolder scans for " + oF.getLabel(),
					oF.getSpoolEvents(),
					oF.getJobsPriority(),
					() -> {
						folderActivity.onStartScan(oF);
						service.enable();
						if (service.isHasFirstStarted() == false) {
							service.runFirstOnStartup();
						}
					}, this::justLogAfterBadUserRun);
		});
	}

	public synchronized void stopScans() {
		observedFoldersServices.values().stream()
				.forEach(BackgroundService::disable);
	}

}
