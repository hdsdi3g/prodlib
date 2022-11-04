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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.MockUtil.isMock;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.util.Locale;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.mailkit.mod.configuration.MailKitConfig;

/**
 * @see TranslateDisableSaveMissingMessagesTest
 */
@SpringBootTest(value = { "mailkit.saveMissingMessages: target/mailkit" })
class TranslateTest {
	static Faker faker = Faker.instance();

	@Autowired
	Translate translate;
	@MockBean
	ResourceBundleMessageSource messageSource;
	@Autowired
	private MailKitConfig conf;

	@Mock
	SupervisableEndEvent event;
	Locale lang;
	String code;
	String defaultResult;
	String[] vars;
	String eventTypeName;
	String resultMessage;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		assertTrue(isMock(messageSource));

		lang = faker.options().option(Locale.getAvailableLocales());
		code = faker.numerify("code###");
		defaultResult = "DRESULT" + faker.hobbit().location();
		vars = faker.collection(() -> faker.book().title())
				.len(1, 20)
				.generate()
				.toArray(new String[0]);
		eventTypeName = faker.numerify("eventtypename###");
		when(event.typeName()).thenReturn(eventTypeName);
		resultMessage = "RESULTMESSAGE" + faker.animal().name();

		assertNotNull(conf.getSaveMissingMessages());
	}

	@AfterEach
	void end() {
		verify(event, atLeast(0)).typeName();
		verifyNoMoreInteractions(messageSource, event);
	}

	@Test
	void testMakeKeyByEvent() {
		final var keyByEvent = translate.makeKeyByEvent(eventTypeName, code);
		assertNotNull(keyByEvent);
		assertTrue(keyByEvent.contains(eventTypeName));
		assertTrue(keyByEvent.contains(code));
		assertTrue(keyByEvent.startsWith("mailkit.notification"));
	}

	@Test
	void testGetMissingMessageFile() {
		final var f0 = translate.getMissingMessageFile(lang);
		assertNotNull(f0);

		var otherLang = lang;
		while (lang.equals(otherLang)) {
			otherLang = faker.options().option(Locale.getAvailableLocales());
		}
		final var f1 = translate.getMissingMessageFile(otherLang);
		assertNotNull(f1);
		assertNotEquals(f0, f1);
	}

	@Nested
	class I18n {
		String keyByEvent;

		@BeforeEach
		void init() throws Exception {
			keyByEvent = translate.makeKeyByEvent(eventTypeName, code);
		}

		@AfterEach
		void end() {
			verify(messageSource, times(1)).getMessage(keyByEvent, vars, lang);
		}

		@Test
		void testSimple() {
			when(messageSource.getMessage(keyByEvent, vars, lang)).thenReturn(resultMessage);

			final var result = translate.i18n(lang, event, code, defaultResult, vars);
			assertEquals(resultMessage, result);
		}

		@Test
		void testInvalidDefaultResult() {
			when(messageSource.getMessage(keyByEvent, vars, lang)).thenThrow(NoSuchMessageException.class);

			assertThrows(IllegalArgumentException.class,
					() -> translate.i18n(lang, event, code, defaultResult + "{}", vars));
		}

		@Test
		void testDefaultResult() {
			when(messageSource.getMessage(keyByEvent, vars, lang)).thenThrow(NoSuchMessageException.class);
			when(messageSource.getMessage("mailkit.notification." + code, vars, lang)).thenReturn(resultMessage);

			final var result = translate.i18n(lang, event, code, defaultResult, vars);
			assertEquals(resultMessage, result);
			verify(messageSource, times(1)).getMessage("mailkit.notification." + code, vars, lang);
		}

		@Test
		void testWithoutResult() throws IOException {
			when(messageSource.getMessage(keyByEvent, vars, lang)).thenThrow(NoSuchMessageException.class);
			when(messageSource.getMessage("mailkit.notification." + code, vars, lang))
					.thenThrow(NoSuchMessageException.class);
			when(messageSource.getMessage("mailkit.notification." + code, vars, defaultResult, lang))
					.thenReturn(resultMessage);

			final var missingFile = translate.getMissingMessageFile(lang);
			if (missingFile.exists()) {
				FileUtils.forceDelete(missingFile);
			}

			final var result = translate.i18n(lang, event, code, defaultResult, vars);
			assertEquals(resultMessage, result);
			verify(messageSource, times(1)).getMessage("mailkit.notification." + code, vars, lang);
			verify(messageSource, times(1)).getMessage("mailkit.notification." + code, vars, defaultResult, lang);

			assertTrue(missingFile.exists());
			assertTrue(missingFile.length() > 0);
			assertTrue(missingFile.isFile());
			FileUtils.forceDelete(missingFile);
		}

	}

}
