/*
 * This file is part of mailkit.
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
package tv.hd3g.mailkit.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.SupervisableEventRegister;

@Slf4j
public class NotificationManager {
	private final List<NotificationRouter> routers;

	public NotificationManager() {
		routers = Collections.synchronizedList(new ArrayList<>());
	}

	public NotificationManager register(final SupervisableEventRegister supervisable) {
		Objects.requireNonNull(supervisable, "\"supervisable\" can't to be null");
		if (routers.isEmpty()) {
			throw new IllegalStateException("Can't register SupervisableManager: no Router is registed.");
		}
		log.info("Register SupervisableManager: {}", supervisable);

		supervisable.registerOnEndEventConsumer(event -> {
			log.trace("Send event to router(s): {}", event);
			routers.forEach(r -> r.send(event));
		});
		return this;
	}

	public NotificationManager register(final NotificationRouter router) {
		Objects.requireNonNull(router, "\"router\" can't to be null");
		log.debug("Register NotificationRouter: {}", router);
		routers.add(router);
		return this;
	}

}
