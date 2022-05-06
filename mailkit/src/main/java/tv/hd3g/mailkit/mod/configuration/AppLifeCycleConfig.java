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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.mailkit.mod.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mailkit.applifecycle")
public class AppLifeCycleConfig {

	/**
	 * An hostname to display in applifecycle mails
	 */
	private String hostname;
	/**
	 * List of all admin mails addresses to send applifecycle mails
	 */
	private List<String> mailAddrAdmin;
	/**
	 * The send mail address who send applifecycle mails
	 */
	private String mailSender;

	public String getHostname() {
		return hostname;
	}

	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	public List<String> getMailAddrAdmin() {
		return mailAddrAdmin;
	}

	public void setMailAddrAdmin(final List<String> mailAddrAdmin) {
		this.mailAddrAdmin = mailAddrAdmin;
	}

	public String getMailSender() {
		return mailSender;
	}

	public void setMailSender(final String mailSender) {
		this.mailSender = mailSender;
	}

}
