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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.EVENT_NOTICE;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.URGENT;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.INTERNAL_STATE_CHANGE;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.SECURITY;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.TRIVIAL;
import static tv.hd3g.mailkit.notification.implmail.NotificationRouterMail.USER_AGENT;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jakarta.mail.MessagingException;
import net.datafaker.Faker;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.jobkit.engine.SupervisableContextExtractor;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.jobkit.engine.SupervisableEventMark;
import tv.hd3g.jobkit.engine.SupervisableManager;
import tv.hd3g.mailkit.mod.service.AppNotificationService;
import tv.hd3g.mailkit.notification.NotificationGroup;
import tv.hd3g.mailkit.notification.SupervisableUtility;
import tv.hd3g.mailkit.utility.FlatJavaMailSender;

class NotificationRouterMailTest {
	static Faker faker = Faker.instance();

	NotificationRouterMail r;
	NotificationEngineMailSetup setup;
	String senderAddr;
	String replyToAddr;
	FlatJavaMailSender mailSender;
	Map<String, Locale> users;
	String userAddr;
	String groupAddr0;
	String groupAddr1;
	Set<String> groupAddr;
	Locale lang;
	Locale langGroup;
	NotificationMailMessage message;
	SupervisableUtility supervisableUtility;
	SupervisableEndEvent event;
	NotificationGroup groupDev;
	NotificationGroup groupAdmin;
	NotificationGroup groupSecurity;

	@Mock
	NotificationMailMessageProducer engineSimpleTemplate;
	@Mock
	NotificationMailMessageProducer engineFullTemplate;
	@Mock
	NotificationMailMessageProducer engineDebugTemplate;
	@Mock
	SupervisableManager supervisableManager;
	@Mock
	AppNotificationService appNotificationService;
	@Mock
	SupervisableContextExtractor contextExtractor;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		senderAddr = faker.numerify("senderAddr###");
		replyToAddr = faker.numerify("replyToAddr###");
		mailSender = new FlatJavaMailSender(false);
		userAddr = faker.name().username();
		groupAddr0 = faker.name().username();
		groupAddr1 = faker.name().username();
		lang = SupervisableUtility.getLang();

		users = Map.of(userAddr, lang);
		message = new NotificationMailMessage(faker.numerify("subject###"), faker.numerify("htmlMessage###"));
		groupAddr = Set.of(groupAddr0, groupAddr1);
		langGroup = SupervisableUtility.getLang();
		groupDev = new NotificationGroup(groupAddr, langGroup);
		groupAdmin = new NotificationGroup(groupAddr, langGroup);
		groupSecurity = new NotificationGroup(groupAddr, langGroup);
	}

	@AfterEach
	void end() {
		mailSender.checkIsMessageListEmpty();
		supervisableUtility.verifyNoMoreInteractions();

		Mockito.verifyNoMoreInteractions(
				engineSimpleTemplate,
				engineFullTemplate,
				engineDebugTemplate,
				supervisableManager,
				appNotificationService,
				contextExtractor);
	}

	void init(final boolean hasGroups, final SupervisableUtility supervisableUtility) {
		setup = new NotificationEngineMailSetup(
				supervisableManager,
				appNotificationService,
				mailSender,
				senderAddr,
				replyToAddr,
				hasGroups ? groupDev : null,
				hasGroups ? groupAdmin : null,
				hasGroups ? groupSecurity : null);
		r = new NotificationRouterMail(
				engineSimpleTemplate,
				engineFullTemplate,
				engineDebugTemplate,
				setup);

		this.supervisableUtility = supervisableUtility;
		event = supervisableUtility.event;

		when(supervisableManager.createContextExtractor(event)).thenReturn(contextExtractor);
		when(appNotificationService.getEndUserContactsToSendEvent(event, contextExtractor)).thenReturn(users);
		when(engineSimpleTemplate.makeMessage(any(), eq(event))).thenReturn(message);
		when(engineFullTemplate.makeMessage(any(), eq(event))).thenReturn(message);
		when(engineDebugTemplate.makeMessage(any(), eq(event))).thenReturn(message);
		when(engineSimpleTemplate.makeMessage(any(), eq(event))).thenReturn(message);
		when(engineFullTemplate.makeMessage(any(), eq(event))).thenReturn(message);
		when(engineDebugTemplate.makeMessage(any(), eq(event))).thenReturn(message);
	}

	@Nested
	class NoGroups {
		final boolean hasGroup = false;

		@BeforeEach
		void nestedInit() throws Exception {
			when(appNotificationService.isSecurityEvent(event)).thenReturn(false);
			when(appNotificationService.isStateChangeEvent(event)).thenReturn(false);
		}

		@Nested
		class SendAMail {

			@AfterEach
			void end() {
				verify(supervisableManager, times(1)).createContextExtractor(event);
				verify(appNotificationService, times(1)).isSecurityEvent(event);
				verify(appNotificationService, times(1)).isStateChangeEvent(event);
				verify(appNotificationService, times(1)).getEndUserContactsToSendEvent(event, contextExtractor);
				verify(engineSimpleTemplate, times(1)).makeMessage(any(), eq(event));
			}

			@Test
			void testSend_OkKoEvent() throws MessagingException {
				init(hasGroup, SupervisableUtility.withMarks(Set.of(), false));

				r.send(event);

				final var mimeMessage = mailSender.getMessagesAndReset(1).get(0);
				assertEquals(message.htmlMessage(), mimeMessage.getMailContent());

				mimeMessage.checkHeaders(senderAddr, replyToAddr, Set.of(userAddr), lang, USER_AGENT, EVENT_NOTICE);
			}

			@Test
			void testSend_ErrorEvent() throws MessagingException {
				init(hasGroup, SupervisableUtility.withMarks(Set.of(), true));

				r.send(event);

				final var mimeMessage = mailSender.getMessagesAndReset(1).get(0);
				assertEquals(message.htmlMessage(), mimeMessage.getMailContent());

				mimeMessage.checkHeaders(senderAddr, replyToAddr, Set.of(userAddr), lang, USER_AGENT, EVENT_NOTICE);
			}
		}

		@Test
		void testSend_EmptyTrivial() throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(TRIVIAL), false));

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_InternalStateChangeEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(INTERNAL_STATE_CHANGE), hasError));

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_InternalSecurityEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(SECURITY), hasError));

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_StateChangeEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(), hasError));
			when(appNotificationService.isStateChangeEvent(event)).thenReturn(true);

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_SecurityEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(), hasError));
			when(appNotificationService.isSecurityEvent(event)).thenReturn(true);

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
		}
	}

	@Nested
	class WithGroups {
		final boolean hasGroup = true;

		@BeforeEach
		void nestedInit() throws Exception {
			when(appNotificationService.isSecurityEvent(event)).thenReturn(false);
			when(appNotificationService.isStateChangeEvent(event)).thenReturn(false);
		}

		@Test
		void testSend_OkKoEvent() throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(), false));

			r.send(event);

			final var mimeMessages = mailSender.getMessagesAndReset(2);

			assertEquals(message.htmlMessage(), mimeMessages.get(0).getMailContent());
			mimeMessages.get(0).checkHeaders(senderAddr, replyToAddr, Set.of(userAddr), lang, USER_AGENT, EVENT_NOTICE);

			assertEquals(message.htmlMessage(), mimeMessages.get(1).getMailContent());
			mimeMessages.get(1).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT, EVENT_NOTICE);

			verify(supervisableManager, times(1)).createContextExtractor(event);
			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
			verify(appNotificationService, times(1)).getEndUserContactsToSendEvent(event, contextExtractor);
			verify(engineSimpleTemplate, times(1)).makeMessage(any(), eq(event));
			verify(engineFullTemplate, times(1)).makeMessage(any(), eq(event));
		}

		@Test
		void testSend_ErrorEvent() throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(), true));

			r.send(event);

			final var mimeMessages = mailSender.getMessagesAndReset(3);

			assertEquals(message.htmlMessage(), mimeMessages.get(0).getMailContent());
			mimeMessages.get(0).checkHeaders(senderAddr, replyToAddr, Set.of(userAddr), lang, USER_AGENT, EVENT_NOTICE);

			assertEquals(message.htmlMessage(), mimeMessages.get(1).getMailContent());
			mimeMessages.get(1).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT, EVENT_NOTICE);

			assertEquals(message.htmlMessage(), mimeMessages.get(2).getMailContent());
			mimeMessages.get(2).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT, EVENT_NOTICE);

			verify(supervisableManager, times(1)).createContextExtractor(event);
			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
			verify(appNotificationService, times(1)).getEndUserContactsToSendEvent(event, contextExtractor);
			verify(engineSimpleTemplate, times(1)).makeMessage(any(), eq(event));
			verify(engineFullTemplate, times(1)).makeMessage(any(), eq(event));
			verify(engineDebugTemplate, times(1)).makeMessage(any(), eq(event));
		}

		@Test
		void testSend_EmptyTrivial() throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(TRIVIAL), false));

			r.send(event);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_InternalStateChangeEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(INTERNAL_STATE_CHANGE), true));

			r.send(event);

			final var mimeMessages = mailSender.getMessagesAndReset(2);

			assertEquals(message.htmlMessage(), mimeMessages.get(0).getMailContent());
			mimeMessages.get(0).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT, EVENT_NOTICE);

			assertEquals(message.htmlMessage(), mimeMessages.get(1).getMailContent());
			mimeMessages.get(1).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT, EVENT_NOTICE);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(appNotificationService, times(1)).isStateChangeEvent(event);
			verify(engineFullTemplate, times(1)).makeMessage(any(), eq(event));
			verify(engineDebugTemplate, times(1)).makeMessage(any(), eq(event));
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void testSend_InternalSecurityEvent(final boolean hasError) throws MessagingException {
			init(hasGroup, SupervisableUtility.withMarks(Set.of(SECURITY), true));

			r.send(event);

			final var mimeMessages = mailSender.getMessagesAndReset(3);

			assertEquals(message.htmlMessage(), mimeMessages.get(0).getMailContent());
			mimeMessages.get(0).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT,
					MessageGrade.SECURITY);

			assertEquals(message.htmlMessage(), mimeMessages.get(1).getMailContent());
			mimeMessages.get(1).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT,
					MessageGrade.SECURITY);

			assertEquals(message.htmlMessage(), mimeMessages.get(2).getMailContent());
			mimeMessages.get(2).checkHeaders(senderAddr, replyToAddr, groupAddr, langGroup, USER_AGENT,
					MessageGrade.SECURITY);

			verify(appNotificationService, times(1)).isSecurityEvent(event);
			verify(engineFullTemplate, times(2)).makeMessage(any(), eq(event));
			verify(engineDebugTemplate, times(1)).makeMessage(any(), eq(event));
		}

	}

	@Test
	void testSend_UrgentEvent() throws MessagingException {
		when(appNotificationService.isSecurityEvent(event)).thenReturn(false);
		when(appNotificationService.isStateChangeEvent(event)).thenReturn(false);

		init(false, SupervisableUtility.withMarks(Set.of(SupervisableEventMark.URGENT), false));

		r.send(event);

		final var mimeMessage = mailSender.getMessagesAndReset(1).get(0);
		assertEquals(message.htmlMessage(), mimeMessage.getMailContent());

		mimeMessage.checkHeaders(senderAddr, replyToAddr, Set.of(userAddr), lang, USER_AGENT, URGENT);

		verify(supervisableManager, times(1)).createContextExtractor(event);
		verify(appNotificationService, times(1)).isSecurityEvent(event);
		verify(appNotificationService, times(1)).isStateChangeEvent(event);
		verify(appNotificationService, times(1)).getEndUserContactsToSendEvent(event, contextExtractor);
		verify(engineSimpleTemplate, times(1)).makeMessage(any(), eq(event));
	}

}
