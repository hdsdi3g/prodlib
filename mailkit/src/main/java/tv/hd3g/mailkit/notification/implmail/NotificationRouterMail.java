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

import static java.util.Objects.requireNonNull;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.SECURITY;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.mail.javamail.MimeMessageHelper;

import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.mailkit.notification.NotificationGroup;
import tv.hd3g.mailkit.notification.NotificationRouter;

public class NotificationRouterMail implements NotificationRouter {
	public static final String USER_AGENT = "JavaMail/MailKit/Notification";

	private static Logger log = LogManager.getLogger();

	private final NotificationMailMessageProducer engineSimpleTemplate;
	private final NotificationMailMessageProducer engineFullTemplate;
	private final NotificationMailMessageProducer engineDebugTemplate;
	private final Optional<NotificationGroup> oGroupDev;
	private final Optional<NotificationGroup> oGroupAdmin;
	private final Optional<NotificationGroup> oGroupSecu;
	private final NotificationEngineMailSetup setup;

	public NotificationRouterMail(final NotificationMailMessageProducer engineSimpleTemplate,
								  final NotificationMailMessageProducer engineFullTemplate,
								  final NotificationMailMessageProducer engineDebugTemplate,
								  final NotificationEngineMailSetup setup) {
		this.engineSimpleTemplate = requireNonNull(engineSimpleTemplate,
				"\"engineSimpleTemplate\" can't to be null");
		this.engineFullTemplate = requireNonNull(engineFullTemplate,
				"\"engineFullTemplate\" can't to be null");
		this.engineDebugTemplate = requireNonNull(engineDebugTemplate,
				"\"engineDebugTemplate\" can't to be null");
		oGroupDev = Optional.ofNullable(setup.groupDev());
		oGroupAdmin = Optional.ofNullable(setup.groupAdmin());
		oGroupSecu = Optional.ofNullable(setup.groupSecurity());
		this.setup = requireNonNull(setup, "\"setup\" can't to be null");

	}

	@Override
	public void send(final SupervisableEndEvent event) {
		if (setup.appNotificationService().isSecurityEvent(event) || event.isSecurityMarked()) {
			log.trace("Route sended event {}: security", event);
			sendSecurityEvent(event);
		} else if (setup.appNotificationService().isStateChangeEvent(event) || event.isInternalStateChangeMarked()) {
			log.trace("Route sended event {}: state change", event);
			sendStateChangeEvent(event);
		} else if (event.error() != null) {
			log.trace("Route sended event {}: error", event);
			sendErrorEvent(event);
		} else if (event.isNotTrivialMarked()) {
			log.trace("Route sended event {}: ok/ko", event);
			sendOkKoEvent(event);
		} else {
			log.debug("Don't send event, not interesting enough: {}", event);
		}
	}

	private void sendOkKoEvent(final SupervisableEndEvent event) {
		final var endUserscontacts = setup.appNotificationService()
				.getEndUserContactsToSendEvent(event, setup.supervisableManager().createContextExtractor(event));

		endUserscontacts.forEach((lang, addr) -> send(lang, addr, engineSimpleTemplate, event));

		if (oGroupAdmin.isPresent()) {
			send(oGroupAdmin.get().addrList(), oGroupAdmin.get().lang(), engineFullTemplate, event);
		}
	}

	void sendErrorEvent(final SupervisableEndEvent event) {
		final var endUserscontacts = setup.appNotificationService()
				.getEndUserContactsToSendEvent(event, setup.supervisableManager().createContextExtractor(event));

		endUserscontacts.forEach((lang, addr) -> send(lang, addr, engineSimpleTemplate, event));

		if (oGroupAdmin.isPresent()) {
			send(oGroupAdmin.get().addrList(), oGroupAdmin.get().lang(), engineFullTemplate, event);
		}
		if (oGroupDev.isPresent()) {
			send(oGroupDev.get().addrList(), oGroupDev.get().lang(), engineDebugTemplate, event);
		}
	}

	void sendStateChangeEvent(final SupervisableEndEvent event) {
		if (oGroupAdmin.isPresent()) {
			send(oGroupAdmin.get().addrList(), oGroupAdmin.get().lang(), engineFullTemplate, event);
		}
		if (oGroupDev.isPresent()) {
			send(oGroupDev.get().addrList(), oGroupDev.get().lang(), engineDebugTemplate, event);
		}
	}

	void sendSecurityEvent(final SupervisableEndEvent event) {
		if (oGroupAdmin.isPresent()) {
			send(oGroupAdmin.get().addrList(), oGroupAdmin.get().lang(), engineFullTemplate, event, SECURITY);
		}
		if (oGroupSecu.isPresent()) {
			send(oGroupSecu.get().addrList(), oGroupSecu.get().lang(), engineFullTemplate, event, SECURITY);
		}
		if (oGroupDev.isPresent()) {
			send(oGroupDev.get().addrList(), oGroupDev.get().lang(), engineDebugTemplate, event, SECURITY);
		}
	}

	private void send(final String recipientsAddr,
					  final Locale lang,
					  final NotificationMailMessageProducer template,
					  final SupervisableEndEvent event) {
		this.send(Set.of(recipientsAddr), lang, template, event);
	}

	private void send(final Set<String> recipientsAddr,
					  final Locale lang,
					  final NotificationMailMessageProducer template,
					  final SupervisableEndEvent event) {
		MessageGrade grade;
		if (event.isSecurityMarked()) {
			grade = MessageGrade.SECURITY;
		} else if (event.isUrgentMarked()) {
			grade = MessageGrade.URGENT;
		} else {
			grade = MessageGrade.EVENT_NOTICE;
		}
		send(recipientsAddr, lang, template, event, grade);
	}

	private void send(final Set<String> recipientsAddr,
					  final Locale lang,
					  final NotificationMailMessageProducer template,
					  final SupervisableEndEvent event,
					  final MessageGrade grade) {
		final var mailMessage = template.makeMessage(lang, event);
		final var mimeMessage = setup.mailSender().createMimeMessage();
		final var subject = mailMessage.subject();
		final var htmlMessage = mailMessage.htmlMessage();

		try {
			final var message = new MimeMessageHelper(mimeMessage, MULTIPART_MODE_MIXED_RELATED, "UTF-8");

			message.setSubject(subject);
			message.setFrom(setup.senderAddr());
			message.setReplyTo(setup.replyToAddr());

			if (recipientsAddr.size() == 1) {
				message.setTo(recipientsAddr.stream().findFirst().orElseThrow());
			} else {
				message.setTo(recipientsAddr.toArray(new String[0]));
			}

			mimeMessage.setHeader("User-Agent", USER_AGENT);
			mimeMessage.setHeader("Content-Language", lang.toString());
			mimeMessage.setHeader("X-Notification-type", event.typeName());
			message.setPriority(grade.getMessagePriority());
			message.setText(htmlMessage, true);
			log.trace("Full message: {}", htmlMessage);

			log.info("Send a mail to {}: \"{}\"", recipientsAddr, subject);
			setup.mailSender().send(mimeMessage);
		} catch (final MessagingException e) {
			log.error("Can't send mail to {} ({})", recipientsAddr, subject, e);
			throw new IllegalStateException("Can't send mail ", e);
		}
	}

}