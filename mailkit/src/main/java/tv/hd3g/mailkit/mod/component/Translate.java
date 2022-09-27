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
package tv.hd3g.mailkit.mod.component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.mailkit.mod.configuration.MailKitConfig;

@Component
public class Translate {
	private static final String MAILKIT_NOTIFICATION = "mailkit.notification.";
	private static Logger log = LogManager.getLogger();
	private static ConcurrentMap<Locale, Properties> messagesFiles = new ConcurrentHashMap<>();

	@Autowired
	private MessageSource messageSource;
	@Autowired
	private MailKitConfig conf;

	@PostConstruct
	void init() throws IOException {
		if (conf.getSaveMissingMessages() == null) {
			return;
		}
		if (conf.getSaveMissingMessages().exists() == false) {
			FileUtils.forceMkdir(conf.getSaveMissingMessages());
		}
	}

	String makeKeyByEvent(final String eventTypeName, final String code) {
		return MAILKIT_NOTIFICATION + eventTypeName.toLowerCase() + "." + code;
	}

	public String i18n(final Locale lang,
					   final SupervisableEndEvent event,
					   final String code,
					   final String defaultResult,
					   final String... vars) {
		return i18n(lang, event.typeName(), code, defaultResult, vars);
	}

	public String i18n(final Locale lang,
					   final String eventTypeName,
					   final String code,
					   final String defaultResult,
					   final String... vars) {
		final var keyByEvent = makeKeyByEvent(eventTypeName, code);
		try {
			return messageSource.getMessage(keyByEvent, vars, lang);
		} catch (final NoSuchMessageException e) {
			/**
			 * Try by default now.
			 */
		}
		if (defaultResult.contains("{}")) {
			throw new IllegalArgumentException("Never use \"{}\" as defaultResult, always add a number like {0}");
		}

		final var keyDefault = MAILKIT_NOTIFICATION + code;
		try {
			return messageSource.getMessage(keyDefault, vars, lang);
		} catch (final NoSuchMessageException e) {
			final String key;
			if (eventTypeName.isEmpty()) {
				key = MAILKIT_NOTIFICATION + code;
			} else {
				key = MAILKIT_NOTIFICATION + "(" + eventTypeName + ")." + code;
			}
			log.warn("Can't resolve message code \"{}\" [{}], set by default: \"{}\"", key, lang, defaultResult);
			appendI18nMissingMessageFile(lang, keyDefault, defaultResult);
			return messageSource.getMessage(keyDefault, vars, defaultResult, lang);
		}
	}

	File getMissingMessageFile(final Locale lang) {
		return new File(conf.getSaveMissingMessages(), "example-messages_" + lang.toString() + ".properties");
	}

	private void appendI18nMissingMessageFile(final Locale lang, final String keyDefault, final String defaultResult) {
		if (conf.getSaveMissingMessages() == null) {
			return;
		}
		final var messages = messagesFiles.computeIfAbsent(lang, l -> new Properties());
		final var previous = messages.putIfAbsent(keyDefault, defaultResult);
		if (previous != null) {
			return;
		}
		final var messageFile = getMissingMessageFile(lang);
		log.info("Write to \"{}\" missing file...", messageFile);

		try (final var fw = new FileWriter(messageFile)) {
			messages.store(fw, "Automatically generated by " + getClass().getName());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write to example messageFile", e);
		}
	}

}
