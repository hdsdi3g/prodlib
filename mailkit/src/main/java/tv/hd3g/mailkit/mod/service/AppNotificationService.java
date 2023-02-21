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
package tv.hd3g.mailkit.mod.service;

import java.util.Locale;
import java.util.Map;

import tv.hd3g.jobkit.engine.SupervisableContextExtractor;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;

public interface AppNotificationService extends SendAsSimpleNotificationContextPredicate {

	boolean isStateChangeEvent(SupervisableEndEvent event);

	boolean isSecurityEvent(SupervisableEndEvent event);

	/**
	 * @return map of email address (and lang to apply), like "me@localhost" or "Me <me@localhost>" to contact for notify this event, or null.
	 */
	Map<String, Locale> getEndUserContactsToSendEvent(SupervisableEndEvent event,
													  SupervisableContextExtractor contextExtractor);

	default String getMessageSourceBasename() {
		return null;
	}

}
