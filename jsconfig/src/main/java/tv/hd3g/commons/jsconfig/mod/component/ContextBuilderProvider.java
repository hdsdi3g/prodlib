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
package tv.hd3g.commons.jsconfig.mod.component;

import static org.graalvm.polyglot.HostAccess.ALL;

import java.io.File;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.EnvironmentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tv.hd3g.commons.jsconfig.JSContextLoader;
import tv.hd3g.commons.jsconfig.mod.JSConfigConfig;

@Component
public class ContextBuilderProvider {

	static {
		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
	}

	@Autowired
	private JSConfigConfig config;

	Builder newBuilder() {
		return Context.newBuilder("js")
				.allowHostAccess(ALL)
				.allowHostClassLookup(className -> true)
				.allowCreateProcess(config.isAllowCreateProcess())
				.allowCreateThread(config.isAllowCreateThread())
				.allowEnvironmentAccess(EnvironmentAccess.INHERIT)
				.allowExperimentalOptions(config.isAllowExperimentalOptions())
				.allowHostClassLoading(config.isDisableHostClassLoading() == false)
				.allowIO(config.isAllowIO())
				.allowNativeAccess(config.isAllowNativeAccess());
	}

	public JSContextLoader newContextLoader(final Set<File> jsFiles) {
		return new JSContextLoader(jsFiles, config, newBuilder());
	}

}
