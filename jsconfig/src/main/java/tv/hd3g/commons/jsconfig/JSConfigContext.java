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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.graalvm.polyglot.HostAccess.ALL;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.Value;

import lombok.Getter;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

@Getter
public class JSConfigContext {
	private static final Logger log = LogManager.getLogger();

	static {
		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
	}

	private final Context context;
	private final Value binding;

	public JSConfigContext(final Set<File> jsFiles, final JSConfigConfig config) {
		final var allJsContent = jsFiles.stream()
				.map(File::getAbsoluteFile)
				.sorted()
				.map(f -> {
					try {
						log.info("Load JS file {} ({} bytes) {}",
								f, f.length(), new Date(f.lastModified()));
						return FileUtils.readFileToString(f, UTF_8);
					} catch (final IOException e) {
						throw new UncheckedIOException("Can't read file " + f, e);
					}
				})
				.map(t -> t.replace("\r", ""))
				.collect(Collectors.joining("\n"));

		final var contextBuilder = Context.newBuilder("js")
				.allowHostAccess(ALL)
				.allowHostClassLookup(className -> true)
				.allowCreateProcess(config.isAllowCreateProcess())
				.allowCreateThread(config.isAllowCreateThread())
				.allowEnvironmentAccess(EnvironmentAccess.INHERIT)
				.allowExperimentalOptions(config.isAllowExperimentalOptions())
				.allowHostClassLoading(config.isDisableHostClassLoading() == false)
				.allowIO(config.isAllowIO())
				.allowNativeAccess(config.isAllowNativeAccess());

		if (config.isFineLevelLogger()) {
			contextBuilder
					.option("log.level", "FINE")
					.option("log.js.level", "FINE")
					.option("log.js.com.oracle.truffle.js.parser.JavaScriptLanguage.level", "FINE");
		}

		Optional.ofNullable(config.getCurrentWorkingDirectory())
				.filter(File::exists)
				.filter(File::isDirectory)
				.map(File::getAbsoluteFile)
				.map(File::toPath)
				.ifPresent(contextBuilder::currentWorkingDirectory);

		context = contextBuilder.build();
		context.eval("js", allJsContent);
		binding = context.getBindings("js");
	}

	public void close() {
		context.close();
	}
}
