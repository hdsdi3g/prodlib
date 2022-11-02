/*
 * This file is part of jobkit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.mod.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Component
@Slf4j
public class JobKitOnBootStartsBackgServices {
	@Autowired
	private JobKitEngine jobKitEngine;

	@EventListener(ApplicationReadyEvent.class)
	public void startsBackgroundServices() {
		log.debug("App has booted, start background services...");
		jobKitEngine.onApplicationReadyRunBackgroundServices();
	}

}
