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

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.graalvm.polyglot.Value;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;
import tv.hd3g.commons.jsconfig.mod.component.ContextBuilderProvider;

/**
 * Thread safe
 */
@Slf4j
public class JSContextRunner {
	private final ContextBuilderProvider contextBuilderProvider;
	private final ReentrantReadWriteLock lock;
	private final Set<File> fileSrc;
	private final Set<File> dirSrc;

	private JSContextLoader jsContextLoader;

	public JSContextRunner(final JSConfigConfig config,
						   final ContextBuilderProvider contextBuilderProvider,
						   final JSUpdateWatcher watcher) {
		this.contextBuilderProvider = contextBuilderProvider;
		lock = new ReentrantReadWriteLock();

		final var fileDirSrc = config.getFileDirSrc();
		fileSrc = config.getFileSrc();
		dirSrc = config.getDirSrc();
		if (fileDirSrc.isEmpty()) {
			return;
		}
		updateContext();

		if (config.isDisableWatchfolder() == false) {
			watcher.setOnUpdate(this::updateContext);
			watcher.start();
		}
	}

	void updateContext() {
		lock.writeLock().lock();
		try {
			final var jsFiles = Stream.concat(
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
			log.debug("Load new JS config file: {}", jsFiles);
			if (jsContextLoader != null) {
				jsContextLoader.close();
			}
			jsContextLoader = contextBuilderProvider.newContextLoader(jsFiles);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Optional<Value> executeJSFunction(final String name, final Object... arguments) {
		lock.readLock().lock();
		try {
			if (jsContextLoader == null) {
				return Optional.empty();
			}

			log.trace("Start to run {}", name);
			final var member = jsContextLoader.getBinding().getMember(name);
			if (member == null) {
				log.warn("Can't run {}(): memberName don't exists", name);
				return Optional.empty();
			}

			final var result = member.execute(arguments);
			log.info("Run {}() return {}", name, result);

			return Optional.ofNullable(result);
		} finally {
			lock.readLock().unlock();
		}
	}

	public Set<String> getMemberKeys() {
		lock.readLock().lock();
		try {
			if (jsContextLoader == null) {
				return Set.of();
			}
			final var binding = jsContextLoader.getBinding();
			return binding.getMemberKeys();
		} finally {
			lock.readLock().unlock();
		}
	}

}
