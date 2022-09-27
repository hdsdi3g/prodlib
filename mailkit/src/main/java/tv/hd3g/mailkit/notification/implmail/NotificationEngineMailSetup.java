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
package tv.hd3g.mailkit.notification.implmail;

import org.springframework.mail.javamail.JavaMailSender;

import tv.hd3g.jobkit.engine.SupervisableManager;
import tv.hd3g.mailkit.mod.service.AppNotificationService;
import tv.hd3g.mailkit.notification.NotificationGroup;

public record NotificationEngineMailSetup(SupervisableManager supervisableManager,
										  AppNotificationService appNotificationService,
										  JavaMailSender mailSender,
										  String senderAddr,
										  String replyToAddr,
										  NotificationGroup groupDev,
										  NotificationGroup groupAdmin,
										  NotificationGroup groupSecurity) {

}
