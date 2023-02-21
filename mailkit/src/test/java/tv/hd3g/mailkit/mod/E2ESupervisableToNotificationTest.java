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
package tv.hd3g.mailkit.mod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.MockUtil.isMock;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.EVENT_NOTICE;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.SECURITY;
import static tv.hd3g.mailkit.notification.implmail.NotificationRouterMail.USER_AGENT;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import net.datafaker.Faker;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.RunnableWithException;
import tv.hd3g.jobkit.engine.Supervisable;
import tv.hd3g.mailkit.mod.configuration.MailKitConfig;
import tv.hd3g.mailkit.mod.service.AppNotificationService;
import tv.hd3g.mailkit.notification.SupervisableUtility;
import tv.hd3g.mailkit.utility.FlatJavaMailSender;
import tv.hd3g.mailkit.utility.MimeMessageAnalyzer;

@SpringBootTest
@TestPropertySource(locations = { "classpath:usergroups.properties" })
class E2ESupervisableToNotificationTest {
	static Faker faker = Faker.instance();

	@MockBean
	AppNotificationService appNotificationService;
	@Autowired
	FlatJavaMailSender mailSender;
	@Autowired
	JobKitEngine engine;
	@Autowired
	MailKitConfig config;

	Locale lang;
	Locale groupLang;

	String userInternalAddr;
	String userAdminAddr;
	String userDevAddr;
	String userSecAddr;

	String typeName;
	String translateResult;
	String context;
	String jobName;
	String spoolName;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		assertFalse(isMock(engine));

		userInternalAddr = faker.name().fullName() + " <" + faker.name().username() + ">";
		userAdminAddr = config.getGroupAdmin().addrList().stream().findFirst().get();
		userDevAddr = config.getGroupDev().addrList().stream().findFirst().get();
		userSecAddr = config.getGroupSecurity().addrList().stream().findFirst().get();
		lang = SupervisableUtility.getLang();
		groupLang = config.getGroupAdmin().lang();

		typeName = faker.numerify("typeName###");
		context = faker.numerify("context###");
		jobName = faker.numerify("jobName###");
		spoolName = faker.numerify("spoolName###");

		when(appNotificationService.getEndUserContactsToSendEvent(any(), any()))
				.thenReturn(Map.of(userInternalAddr, lang));
		when(appNotificationService.isSendAsSimpleNotificationThisContextEntry(any(), any()))
				.thenReturn(true);
		mailSender.clear();
	}

	@AfterEach
	void end() {
		mailSender.checkIsMessageListEmpty();
	}

	void checkMail(final MimeMessageAnalyzer message,
				   final String recipient,
				   final Locale lang,
				   final MessageGrade grade) throws MessagingException {
		message.checkHeaders(config.getSenderAddr(), config.getReplyToAddr(),
				Set.of(recipient), lang, USER_AGENT, grade);
	}

	@Test
	void test_ok() throws InterruptedException, MessagingException, IOException {
		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "Test ok")
					.onMessage("test2", "Test ok2")
					.setContext(typeName, getJustForFunContext())
					.resultDone();
		});
		assertNull(e);

		final var m = mailSender.getMessagesAndReset(2);

		checkMail(m.get(0), userInternalAddr, lang, EVENT_NOTICE);
		checkMail(m.get(1), userAdminAddr, groupLang, EVENT_NOTICE);
	}

	@Test
	void test_error() throws InterruptedException, MessagingException, IOException {
		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "error")
					.setContext(typeName, getJustForFunContext());
			throw new IllegalStateException("Just for test: " + faker.disease().internalDisease());
		});
		assertNotNull(e);

		final var m = mailSender.getMessagesAndReset(3);

		checkMail(m.get(0), userInternalAddr, lang, EVENT_NOTICE);
		checkMail(m.get(1), userAdminAddr, groupLang, EVENT_NOTICE);
		checkMail(m.get(2), userDevAddr, groupLang, EVENT_NOTICE);
	}

	@Test
	void test_error_withOk() throws InterruptedException, MessagingException, IOException {
		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "error")
					.setContext(typeName, getJustForFunContext())
					.resultDone();
			throw new IllegalStateException("Just for test: " + faker.disease().internalDisease());
		});
		assertNotNull(e);

		final var m = mailSender.getMessagesAndReset(3);

		checkMail(m.get(0), userInternalAddr, lang, EVENT_NOTICE);
		checkMail(m.get(1), userAdminAddr, groupLang, EVENT_NOTICE);
		checkMail(m.get(2), userDevAddr, groupLang, EVENT_NOTICE);
	}

	@Test
	void test_InternalSecurity() throws InterruptedException, MessagingException, IOException {
		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "InternalSecurity")
					.setContext(typeName, getJustForFunContext())
					.markAsSecurity()
					.resultDone();
		});
		assertNull(e);

		final var m = mailSender.getMessagesAndReset(3);

		checkMail(m.get(0), userAdminAddr, groupLang, SECURITY);
		checkMail(m.get(1), userSecAddr, groupLang, SECURITY);
		checkMail(m.get(2), userDevAddr, groupLang, SECURITY);
	}

	@Test
	void test_InternalStateChange() throws InterruptedException, MessagingException, IOException {
		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "InternalStateChange")
					.setContext(typeName, getJustForFunContext())
					.markAsInternalStateChange()
					.resultDone();
		});
		assertNull(e);

		final var m = mailSender.getMessagesAndReset(2);

		checkMail(m.get(0), userAdminAddr, groupLang, EVENT_NOTICE);
		checkMail(m.get(1), userDevAddr, groupLang, EVENT_NOTICE);
	}

	@Test
	void test_Security() throws InterruptedException, MessagingException, IOException {
		when(appNotificationService.isSecurityEvent(argThat(event -> event.typeName().equals(typeName))))
				.thenReturn(true);

		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "Security")
					.setContext(typeName, getJustForFunContext())
					.resultDone();
		});
		assertNull(e);

		final var m = mailSender.getMessagesAndReset(3);

		checkMail(m.get(0), userAdminAddr, groupLang, SECURITY);
		checkMail(m.get(1), userSecAddr, groupLang, SECURITY);
		checkMail(m.get(2), userDevAddr, groupLang, SECURITY);
	}

	@Test
	void test_StateChange() throws InterruptedException, MessagingException, IOException {
		when(appNotificationService.isStateChangeEvent(argThat(event -> event.typeName().equals(typeName))))
				.thenReturn(true);

		final var e = startRun(() -> {
			Supervisable.getSupervisable()
					.onMessage("test", "StateChange")
					.setContext(typeName, getJustForFunContext())
					.markAsInternalStateChange()
					.resultDone();
		});
		assertNull(e);

		final var m = mailSender.getMessagesAndReset(2);

		checkMail(m.get(0), userAdminAddr, groupLang, EVENT_NOTICE);
		checkMail(m.get(1), userDevAddr, groupLang, EVENT_NOTICE);
	}

	private JustForFunContext getJustForFunContext() {
		return new JustForFunContext(
				context,
				faker.random().nextInt(),
				List.of(faker.numerify("list##"), faker.numerify("list##")),
				Map.of("k0", faker.numerify("map##"), "k1", faker.numerify("map##")));
	}

	record JustForFunContext(String aValue, int aNumber, List<String> list, Map<String, String> map) {
	}

	private Exception startRun(final RunnableWithException run) throws InterruptedException {
		final var latch = new CountDownLatch(2);
		final var exceptionRef = new AtomicReference<Exception>();

		final var started = engine.runOneShot(jobName, spoolName, 0,
				() -> {
					run.run();
					latch.countDown();
				},
				e -> {
					exceptionRef.set(e);
					latch.countDown();
				});
		assertTrue(started);

		latch.await(1, TimeUnit.SECONDS);

		return exceptionRef.get();
	}

}
