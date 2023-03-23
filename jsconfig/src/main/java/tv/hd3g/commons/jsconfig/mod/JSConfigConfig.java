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
package tv.hd3g.commons.jsconfig.mod;

import java.io.File;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jsconfig")
@Data
public class JSConfigConfig {

	/**
	 * JavaScript sources files to import.
	 * Can be some files and directories, sorted by name before load.
	 */
	private List<File> src;

	private boolean allowCreateProcess;
	private boolean allowCreateThread;
	private boolean allowExperimentalOptions;
	private boolean disableHostClassLoading;
	private boolean allowIO;
	private File currentWorkingDirectory;
	private boolean allowNativeAccess;
	private boolean disableEnvironmentAccess;
	private boolean fineLevelLogger;
	private boolean disableWatchfolder;

}
