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
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FilenameUtils.isExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Value;

import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

public class JSConfigRunner {
	private static final Logger log = LogManager.getLogger();

	private final JSConfigConfig config;
	private final Thread watcher;
	private final ReentrantReadWriteLock lock;
	private final CountDownLatch startupLatch;
	private final AtomicInteger updateVersion;
	private final Set<File> fileSrc;
	private final Set<File> dirSrc;

	private JSConfigContext jsConfigLoader;

	public JSConfigRunner(final JSConfigConfig config) {
		this.config = config;
		watcher = new Thread(new Watcher());
		lock = new ReentrantReadWriteLock();
		updateVersion = new AtomicInteger(0);
		startupLatch = new CountDownLatch(1);

		final var fileDirSrc = Optional.ofNullable(config.getSrc()).stream()
				.flatMap(List::stream)
				.distinct()
				.map(f -> {
					if (f.exists() == false) {
						throw new UncheckedIOException(new FileNotFoundException("File/dir don't exists: " + f));
					}
					try {
						return f.getCanonicalFile();
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't access to file/dir", e);
					}
				})
				.toList();

		fileSrc = fileDirSrc.stream()
				.filter(not(File::isDirectory))
				.collect(toUnmodifiableSet());
		dirSrc = fileDirSrc.stream()
				.filter(File::isDirectory)
				.collect(toUnmodifiableSet());

		if (fileDirSrc.isEmpty()) {
			return;
		}
		updateContext();

		if (config.isDisableWatchfolder() == false) {
			watcher.setDaemon(true);
			watcher.setName("JSConfig change watcher");
			watcher.setPriority(Thread.MIN_PRIORITY);
			watcher.start();
			try {
				startupLatch.await(1, SECONDS);// NOSONAR S899
			} catch (final InterruptedException e) {
				log.warn("Can't wait to let the watch startup run", e);
				Thread.currentThread().interrupt();
			}
		}
	}

	private void updateContext() {
		final var jsFiles = getAllJSFiles();
		log.debug("Load new JS config file set #{} {}", updateVersion.getAndIncrement(), jsFiles);
		if (jsConfigLoader != null) {
			jsConfigLoader.close();
		}
		jsConfigLoader = new JSConfigContext(jsFiles, config);
	}

	Set<File> getAllJSFiles() {
		return Stream.concat(
				fileSrc.stream(),
				dirSrc.stream()
						.flatMap(dir -> {
							try {
								return FileUtils.streamFiles(dir, true, "js", "JS");
							} catch (final IOException e) {
								throw new UncheckedIOException("Can't access to file/dir", e);
							}
						})
						.filter(f -> f.isHidden() == false)
						.filter(f -> f.getName().startsWith(".") == false)
						.map(File::getAbsoluteFile))
				.distinct()
				.collect(toUnmodifiableSet());
	}

	private class Watcher implements Runnable {

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
						lock.writeLock().lock();
						try {
							updateContext();
						} finally {
							lock.writeLock().unlock();
						}
					}
					startupLatch.countDown();
				}
			} catch (final IOException e) {
				log.error("Can't run watch service for JS config files", e);
			} catch (final InterruptedException e) {
				log.error("Interrupted", e);
				Thread.currentThread().interrupt();
			}
		}
	}

	public Optional<Value> executeJSFunction(final String name, final Object... arguments) {
		lock.readLock().lock();
		try {
			if (jsConfigLoader == null) {
				return Optional.empty();
			}

			log.trace("Start to run {}", name);
			final var member = jsConfigLoader.getBinding().getMember(name);
			if (member == null) {
				log.warn("Can't run {}(): member don't exists", name);
				return Optional.empty();
			}

			final var result = member.execute(arguments);
			log.info("Run {}() return {}", name, result);

			return Optional.ofNullable(result);
		} finally {
			lock.readLock().unlock();
		}
	}

	int getLastUpdateVersion() {
		return updateVersion.get();
	}

	public Set<String> getMemberKeys() {
		lock.readLock().lock();
		try {
			if (jsConfigLoader == null) {
				return Set.of();
			}
			final var binding = jsConfigLoader.getBinding();
			return binding.getMemberKeys();
		} finally {
			lock.readLock().unlock();
		}
	}

}
