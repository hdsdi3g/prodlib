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
package tv.hd3g.mailkit.mod.configuration;

import java.io.File;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import tv.hd3g.mailkit.notification.NotificationEnvironment;
import tv.hd3g.mailkit.notification.NotificationGroup;

@Configuration
@ConfigurationProperties(prefix = "mailkit")
@Data
@Validated
public class MailKitConfig {

	@NotNull
	private NotificationEnvironment env;
	@NotBlank
	private String senderAddr;

	private NotificationGroup groupDev;
	private NotificationGroup groupAdmin;
	private NotificationGroup groupSecurity;
	private String replyToAddr;
	private File saveMissingMessages;

}
