/*
 * This file is part of demo.
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
package tv.hd3g.demo;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ThisLog {// NOSONAR

	// @Autowired
	// JobKitEngine jobkit;
	@Autowired
	ScheduledThreadPoolExecutor sch;

	@PostConstruct
	void init() {
		System.out.println(sch.getPoolSize());
		/*	log.error("E {}", "message");
			log.warn("W");
			log.info("I");
			log.trace("T");
			log.debug("D");
		
			jobkit.runOneShot("Mon job", "spool", 0,
					() -> {
						final var s = getSupervisable();
						s.markAsUrgent().resultDone();
						log.info("Ok for supervisable");
					},
					e -> {
						if (e != null) {
							log.error("Job error", e);
						}
					});
					*/
	}

}
