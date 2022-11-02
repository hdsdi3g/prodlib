/*
 * This file is part of jsconfig.
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
package tv.hd3g.commons.jsconfig;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FilenameUtils.isExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

@Slf4j
public class JSUpdateWatcher extends Thread {
	private final CountDownLatch startupLatch;
	private final Set<File> fileSrc;
	private final Set<File> dirSrc;
	private Runnable onUpdate;

	public JSUpdateWatcher(final JSConfigConfig config) {
		fileSrc = config.getFileSrc();
		dirSrc = config.getDirSrc();
		startupLatch = new CountDownLatch(1);
		setDaemon(true);
		setName("JSConfig change watcher");
		setPriority(MIN_PRIORITY);
	}

	void setOnUpdate(final Runnable onUpdate) {
		this.onUpdate = onUpdate;
	}

	@Override
	public void run() {
		try (final var watchService = FileSystems.getDefault().newWatchService()) {
			Stream.concat(
					dirSrc.stream(),
					fileSrc.stream()
							.map(File::getParentFile))
					.distinct()
					.map(File::toPath)
					.forEach(p -> {
						try {
							log.debug("Register watchService: {}", p);
							p.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE, OVERFLOW);
						} catch (final IOException e) {
							log.error("Can't setup watch dir, cancel watch for it: \"{}\"", p, e);
						}
					});

			startupLatch.countDown();
			while (true) {// NOSONAR S2189
				final var key = watchService.take();
				final var rootDir = ((Path) key.watchable()).toFile().getAbsoluteFile();
				final var lastActivity = key.pollEvents().stream()
						.map(event -> (Path) event.context())
						.map(Path::toString)
						.map(p -> new File(rootDir, p))
						.map(File::getAbsoluteFile)
						.distinct()
						.toList();
				key.reset();

				if (log.isTraceEnabled()) {
					final var kind = key.pollEvents().stream()
							.map(event -> event.kind().toString())
							.distinct()
							.toList();
					log.trace("Detect activity on {} ({})", lastActivity, kind);
				}

				final var refresh = lastActivity.stream().anyMatch(foundedFile -> {
					if (foundedFile.isDirectory()) {
						log.debug("Action on directory {}, don't update context", foundedFile);
						return false;
					}
					if (foundedFile.isHidden() || foundedFile.getName().startsWith(".")) {
						log.debug("Founded file is hidden {}, don't update context", foundedFile);
						return false;
					}
					if (isExtension(foundedFile.getName(), "js", "JS") == false) {
						log.debug("Founded file is not a .js file {}, don't update context", foundedFile);
						return false;
					}
					if (fileSrc.contains(foundedFile)) {
						return true;
					}

					log.debug("Founded file {} is not directly watched on {}. Now search it now on {}",
							foundedFile, fileSrc, dirSrc);
					return dirSrc.stream()
							.anyMatch(d -> foundedFile.getParent().startsWith(d.getPath()));
				});

				if (refresh) {
					onUpdate.run();
				}
			}
		} catch (final IOException e) {
			log.error("Can't run watch service for JS config files", e);
		} catch (final InterruptedException e) {
			log.error("Interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	boolean isInitiated() {
		try {
			return startupLatch.await(5, SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
