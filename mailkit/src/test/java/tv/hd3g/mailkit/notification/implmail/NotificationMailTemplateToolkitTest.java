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

import static java.util.TimeZone.getTimeZone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.node.JsonNodeType;

import j2html.tags.DomContent;
import net.datafaker.Faker;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.mailkit.mod.component.Translate;
import tv.hd3g.mailkit.mod.service.SendAsSimpleNotificationContextPredicate;
import tv.hd3g.mailkit.notification.NotificationEnvironment;
import tv.hd3g.mailkit.notification.SupervisableUtility;

class NotificationMailTemplateToolkitTest {
	static Faker faker = net.datafaker.Faker.instance();

	EnvironmentVersion environmentVersion;

	NotificationMailTemplateToolkit t;
	Locale lang;
	SupervisableEndEvent event;
	SupervisableUtility utility;
	NotificationEnvironment env;
	SendAsSimpleNotificationContextPredicate contextPredicate;
	List<DomContent> listBodyContent;
	List<String> listCSSEntries;

	@Mock
	Translate translate;

	private static final TimeZone previous = TimeZone.getDefault();

	@BeforeAll
	static void load() {
		TimeZone.setDefault(getTimeZone("GMT"));
	}

	@BeforeAll
	static void free() {
		TimeZone.setDefault(previous);
	}

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		lang = SupervisableUtility.getLang();
		utility = new SupervisableUtility();
		event = utility.event;
		env = new NotificationEnvironment(
				faker.numerify("appName###"),
				faker.numerify("instanceName"),
				faker.numerify("vendorName"),
				List.of());
		contextPredicate = new SendAsSimpleNotificationContextPredicate() {
			@Override
			public boolean isSendAsSimpleNotificationThisContextEntry(final String contextKey,
																	  final SupervisableEndEvent event) {
				return true;
			}
		};
		environmentVersion = EnvironmentVersion.makeEnvironmentVersion(
				"appVersion", "prodlibVersion", "frameworkVersion");
		t = new NotificationMailTemplateToolkit(translate, env, environmentVersion);
		listBodyContent = new ArrayList<>();
		listCSSEntries = new ArrayList<>();

		when(translate.i18n(eq(lang), eq(event), any(), any(), any()))
				.thenReturn(faker.numerify("translate###"));
		when(translate.i18n(eq(lang), eq(event.typeName()), any(), any(), any()))
				.thenReturn(faker.numerify("translate###"));
		when(translate.i18n(eq(lang), eq(event), any(), any(), any(), any()))
				.thenReturn(faker.numerify("translate###"));
	}

	String checkStr(final String result) {
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(result, result.trim());
		return result;
	}

	@Test
	void testProcessSubject() {
		checkStr(t.processSubject(lang, event));
	}

	@Test
	void testProcessHTMLMessage() {
		final var result = checkStr(t.processHTMLMessage(
				new HtmlCssDocumentPayload(new ArrayList<>(), new ArrayList<>())));
		assertTrue(result.startsWith("<!DOCTYPE html>"));
	}

	@Test
	void testGetResultStateI18n() {
		checkStr(t.getResultStateI18n(lang, event));
	}

	@Test
	void testSpanWrapp() {
		final var attrs = faker.animal().name().replace(" ", "");
		final var textToEscape = faker.aviation().airport();
		final var result = t.spanWrapp("." + attrs, textToEscape);
		assertEquals("<span class=\"" + attrs + "\">" + textToEscape + "</span>", result);
	}

	@Test
	void testFormatLongDate() {
		final var result = t.formatLongDate(new Date(2_000_000_000_000l), Locale.ENGLISH);
		assertThat(result).isEqualTo("Wednesday, May 18, 2033, 3:33:20 AM");
	}

	@Test
	void testFormatShortDate() {
		final var result = t.formatShortDate(new Date(2_000_000_000_000l), Locale.ENGLISH);
		assertThat(result).isEqualTo("5/18/33, 3:33:20 AM");
	}

	@Test
	void testFormatShortTime() {
		final var result = t.formatShortTime(new Date(2_000_000_000_000l), Locale.ENGLISH);
		assertThat(result).isEqualTo("3:33:20 AM");
	}

	@Test
	void testExceptionFormatterToDom_noVerbose() {
		final var e = new Exception(faker.examplify("AAAAAAAAAAA00000000000000"));
		final var result = t.exceptionFormatterToDom(e, false);
		assertNotNull(result);
		final var render = result.render();
		assertTrue(render.contains("div"), render);
		assertTrue(render.contains("stacktrace"), render);
		assertTrue(render.contains(e.getMessage()), render);
	}

	@Test
	void testExceptionFormatterToDom_Verbose() {
		final var e = new Exception(faker.examplify("AAAAAAAAAAA00000000000000"));
		final var result = t.exceptionFormatterToDom(e, true);
		assertNotNull(result);
		final var render = result.render();
		assertTrue(render.contains("div"), render);
		assertTrue(render.contains("stacktrace"), render);
		assertTrue(render.contains(e.getMessage()), render);
		assertTrue(render.contains(getClass().getName()), render);
	}

	@Test
	void testCallerToString() {
		final var firstCaller = new Exception().getStackTrace()[0];
		final var result = t.callerToString(firstCaller);
		assertEquals(getClass().getSimpleName() + ".java L" + firstCaller.getLineNumber(), result);
	}

	@Test
	void testStepLineToString_noVerbose() {
		assertNotNull(t.stepLineToString(lang, event, event.steps().get(0), false));
	}

	@Test
	void testStepLineToString_Verbose() {
		when(event.steps().get(0).caller().getFileName()).thenReturn(faker.numerify("filename###.java"));
		assertNotNull(t.stepLineToString(lang, event, event.steps().get(0), true));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testStepsList_noVerbose(final boolean verbose) {
		when(event.steps().get(0).caller().getFileName()).thenReturn(faker.numerify("filename###.java"));
		t.stepsList(lang, event, verbose, listBodyContent, listCSSEntries);
		assertEquals(1, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentBaseStyles() {
		t.makeDocumentBaseStyles(listCSSEntries);
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentTitleWithoutResult() {
		t.makeDocumentTitleWithoutResult(lang, event, listBodyContent);
		assertEquals(1, listBodyContent.size());
	}

	@Test
	void testMakeDocumentTitleWithResult() {
		t.makeDocumentTitleWithResult(lang, event, listBodyContent);
		assertEquals(2, listBodyContent.size());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testMakeDocumentTitleError_noVerbose(final boolean displayFullStackTrace) {
		t.makeDocumentTitleError(lang, event, listBodyContent, false, displayFullStackTrace);
		assertEquals(2, listBodyContent.size());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testMakeDocumentTitleError_verbose(final boolean displayFullStackTrace) {
		t.makeDocumentTitleError(lang, event, listBodyContent, true, displayFullStackTrace);
		assertEquals(2, listBodyContent.size());
	}

	@Test
	void testMakeDocumentDates() {
		t.makeDocumentDates(lang, event, listBodyContent);
		assertEquals(1, listBodyContent.size());
	}

	@Test
	void testMakeDocumentContext() {
		t.makeDocumentContext(lang, event, listBodyContent, listCSSEntries);
		assertEquals(1, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentSimpleContext_empty() {
		when(event.context().isNull()).thenReturn(true);
		t.makeDocumentSimpleContext(lang, event, listBodyContent, listCSSEntries, contextPredicate);
		assertTrue(listBodyContent.isEmpty());
		assertTrue(listCSSEntries.isEmpty());
	}

	@Test
	void testMakeDocumentSimpleContext() {
		when(event.context().getNodeType()).thenReturn(JsonNodeType.BOOLEAN);
		t.makeDocumentSimpleContext(lang, event, listBodyContent, listCSSEntries, contextPredicate);
		assertEquals(1, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentCallers() {
		t.makeDocumentCallers(lang, event, listBodyContent, listCSSEntries);
		assertEquals(2, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentEventEnv() {
		t.makeDocumentEventEnv(lang, event, listBodyContent, listCSSEntries);
		assertEquals(2, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testMakeDocumentFooter() {
		t.makeDocumentFooter(listBodyContent, listCSSEntries);
		assertEquals(1, listBodyContent.size());
		assertEquals(1, listCSSEntries.size());
	}

	@Test
	void testTranslateMessage() {
		final var message = event.result().message();
		t.translateMessage(lang, event, message);
		verify(translate, times(1)).i18n(lang, event, message.code(), message.defaultResult(), message.getVarsArray());
	}

}
