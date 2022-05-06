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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.mailkit.mod.template;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.mailkit.mod.configuration.AppLifeCycleConfig;
import tv.hd3g.mailkit.mod.service.AppLifeCycleMailService;

/**
 * Don't forget to add "@Service" to extended class...
 */
public abstract class AppLifeCycleMailServiceAbstract implements AppLifeCycleMailService, InitializingBean {
	private static final Logger log = LogManager.getLogger();

	@Autowired
	protected SendMailService sendMailService;
	@Autowired
	private ResourceBundleMessageSource messageSource;
	@Autowired
	private AppLifeCycleConfig config;

	/**
	 * @return like myapp-messages (point on myapp-messages.properties / myapp-messages_fr.properties...)
	 *         or null/empty
	 */
	protected String getMessageSourceBasename() {
		return null;
	}

	/**
	 * @return like "My Wonderfull App"
	 */
	protected abstract String getAppName();

	/**
	 * @return like "My Company / This Business Unit"
	 */
	protected abstract String getAppOrigin();

	@Override
	public final void afterPropertiesSet() throws Exception {
		final var baseName = getMessageSourceBasename();
		if (baseName != null && baseName.isEmpty() == false) {
			messageSource.addBasenames(baseName);
		}
	}

	protected String getMessage(final String code, final String... vars) {
		return messageSource.getMessage(code, vars, Locale.getDefault());
	}

	public void sendMail(final String templateName, final String serviceName, final Map<String, Object> templateVars) {
		final var fullTemplateVars = new HashMap<>(templateVars);
		fullTemplateVars.put("appName", getAppName());
		fullTemplateVars.put("serviceName", serviceName);
		fullTemplateVars.put("hostname", config.getHostname());
		fullTemplateVars.put("origin", getAppOrigin());

		final var mailAddrAdminList = Optional.ofNullable(config.getMailAddrAdmin()).orElse(List.of());
		final var sendMailDto = new SendMailDto(templateName, Locale.getDefault(), fullTemplateVars,
		        config.getMailSender(), mailAddrAdminList, List.of(), List.of());
		sendMailService.sendEmail(sendMailDto);
	}

	@Override
	public void onAppStartupService(final String serviceName) {
		log.debug("Mail event: onAppStartupService on {}", serviceName);
		sendMail("applifecycle-mail-starts", serviceName, Map.of());
	}

	@Override
	public void onAppStopService(final String serviceName) {
		log.debug("Mail event: onAppStopService on {}", serviceName);
		sendMail("applifecycle-mail-stops", serviceName, Map.of());
	}

	/**
	 * @param cause should not be e.getMessage()
	 */
	@Override
	public void onAppGenericError(final String serviceName, final String cause, final Throwable e) {
		log.debug("Mail event: onAppGenericError on {}: {}", serviceName, cause, e);
		sendMail("applifecycle-mail-error", serviceName,
		        Map.of("cause", cause,
		                "exceptionMessage", e.getMessage(),
		                "verbose", "Exception: " + prettifyException(e)));
	}

	/**
	 * @param cause should not be e.getMessage()
	 */
	@Override
	public void onAppCantStartupService(final String serviceName, final String cause, final Throwable e) {
		log.debug("Mail event: onAppCantStartupService on {}: {}", serviceName, cause, e);
		sendMail("applifecycle-mail-cantstarts", serviceName,
		        Map.of("cause", cause,
		                "exceptionMessage", e.getMessage(),
		                "verbose", "Exception: " + prettifyException(e)));
	}

	protected String prettifyException(final Throwable e) {
		final var out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		return out.toString().lines().collect(Collectors.joining("\r\n"));
	}

}
