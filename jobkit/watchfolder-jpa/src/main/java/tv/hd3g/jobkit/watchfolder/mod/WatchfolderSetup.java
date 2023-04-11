/*
 * This file is part of jobkit-watchfolder-jpa.
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
package tv.hd3g.jobkit.watchfolder.mod;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import tv.hd3g.jobkit.watchfolder.WatchedFileSetupManager;

@Configuration
@EnableTransactionManagement
@EntityScan("tv.hd3g.jobkit.watchfolder.mod.entity")
@EnableJpaRepositories("tv.hd3g.jobkit.watchfolder.mod.repository")
public class WatchfolderSetup {

	@Bean
	WatchedFileSetupManager getWatchedFileSetupManager() {
		return new WatchedFileSetupManager();
	}
}
