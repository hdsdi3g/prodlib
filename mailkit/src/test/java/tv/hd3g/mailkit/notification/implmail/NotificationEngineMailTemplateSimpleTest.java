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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import j2html.tags.DomContent;
import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.mailkit.mod.service.SendAsSimpleNotificationContextPredicate;
import tv.hd3g.mailkit.notification.SupervisableUtility;

class NotificationEngineMailTemplateSimpleTest {
	static Faker faker = Faker.instance();

	NotificationMailMessageProducer p;
	NotificationMailMessageProducerEnvironment env;
	String subject;
	String htmlMessage;
	SupervisableEndEvent event;
	SupervisableUtility utility;

	List<DomContent> listBodyContent;
	List<String> listCSSEntries;

	@Mock
	NotificationMailTemplateToolkit toolkit;
	@Captor
	ArgumentCaptor<HtmlCssDocumentPayload> payloadCaptor;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();

		p = new NotificationEngineMailTemplateSimple(toolkit);

		subject = faker.numerify("subject###");
		htmlMessage = faker.numerify("htmlMessage###");
		env = new NotificationMailMessageProducerEnvironment(
				SupervisableUtility.getLang(),
				new SendAsSimpleNotificationContextPredicate() {

					@Override
					public boolean isSendAsSimpleNotificationThisContextEntry(final String contextKey,
																			  final SupervisableEndEvent event) {
						return true;
					}

				});

		when(toolkit.processHTMLMessage(any(HtmlCssDocumentPayload.class))).thenReturn(htmlMessage);
	}

	@AfterEach
	void end() {
		verify(toolkit, times(1)).makeDocumentBaseStyles(listCSSEntries);
		verify(toolkit, times(1)).makeDocumentFooter(listBodyContent, listCSSEntries);

		utility.verifyNoMoreInteractions();
		verifyNoMoreInteractions(toolkit);

		listBodyContent = null;
		listCSSEntries = null;
	}

	void checkSubjectMessage() {
		verify(toolkit, times(1)).processSubject(env.lang(), event);
		verify(toolkit, times(1)).processHTMLMessage(payloadCaptor.capture());
		assertNotNull(payloadCaptor.getValue());
		listBodyContent = payloadCaptor.getValue().listBodyContent();
		assertNotNull(listBodyContent);
		listCSSEntries = payloadCaptor.getValue().listCSSEntries();
		assertNotNull(listCSSEntries);
	}

	void checkMakeMessage() {
		final var mailMessage = p.makeMessage(env, event);
		assertNotNull(mailMessage);
		assertEquals(htmlMessage, mailMessage.htmlMessage());
		assertEquals(subject, mailMessage.subject());
	}

	void setUtility(final SupervisableUtility u) {
		utility = u;
		event = utility.event;
		when(toolkit.processSubject(env.lang(), event)).thenReturn(subject);
	}

	@Test
	void testMakeMessage_noError_noResult_noSteps() {
		setUtility(SupervisableUtility.aLaCarte(false, false, false));

		checkMakeMessage();
		checkSubjectMessage();

		verify(toolkit, times(1)).makeDocumentTitleWithoutResult(env.lang(), event, listBodyContent);
		verify(toolkit, times(1)).makeDocumentSimpleContext(
				env.lang(), event, listBodyContent, listCSSEntries, env.sendAsSimpleNotificationContextPredicate());
	}

	@Test
	void testMakeMessage_Error_noResult_noSteps() {
		setUtility(SupervisableUtility.aLaCarte(true, false, false));

		checkMakeMessage();
		checkSubjectMessage();

		verify(toolkit, times(1)).makeDocumentTitleError(env.lang(), event, listBodyContent, false, false);
		verify(toolkit, times(1)).makeDocumentSimpleContext(
				env.lang(), event, listBodyContent, listCSSEntries, env.sendAsSimpleNotificationContextPredicate());
	}

	@Test
	void testMakeMessage_noError_Result_noSteps() {
		setUtility(SupervisableUtility.aLaCarte(false, true, false));

		checkMakeMessage();
		checkSubjectMessage();

		verify(toolkit, times(1)).makeDocumentTitleWithResult(env.lang(), event, listBodyContent);
		verify(toolkit, times(1)).makeDocumentSimpleContext(
				env.lang(), event, listBodyContent, listCSSEntries, env.sendAsSimpleNotificationContextPredicate());
	}

	@Test
	void testMakeMessage_noError_noResult_Steps() {
		setUtility(SupervisableUtility.aLaCarte(false, false, true));

		checkMakeMessage();
		checkSubjectMessage();

		verify(toolkit, times(1)).makeDocumentTitleWithoutResult(env.lang(), event, listBodyContent);
		verify(toolkit, times(1)).stepsList(env.lang(), event, false, listBodyContent, listCSSEntries);
		verify(toolkit, times(1)).makeDocumentSimpleContext(
				env.lang(), event, listBodyContent, listCSSEntries, env.sendAsSimpleNotificationContextPredicate());
	}

}
