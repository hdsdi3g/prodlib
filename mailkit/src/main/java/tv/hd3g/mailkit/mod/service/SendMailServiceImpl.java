/*
 * This file is part of MailKit.
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
package tv.hd3g.mailkit.mod.service;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailService;

/**
 * File attachement and mail resources are not tested.
 * See https://github.com/dotSwapna/dotEmail.github.io/tree/master/src/main
 */
@Service
@Slf4j
public class SendMailServiceImpl implements SendMailService {

	@Autowired
	private JavaMailSender mailSender;
	@Autowired
	private TemplateEngine htmlTemplateEngine;
	@Autowired
	private TemplateEngine subjectTemplateEngine;

	@Override
	public void sendEmail(final SendMailDto sendMailDto) {
		try {
			internalSendEmail(sendMailDto);
		} catch (final MessagingException e) {
			throw new RuntimeMessagingException(e);
		}
	}

	public static class RuntimeMessagingException extends RuntimeException {
		private RuntimeMessagingException(final MessagingException e) {
			super(e);
		}
	}

	private void internalSendEmail(final SendMailDto sendMailDto) throws MessagingException {
		final var ctx = new Context();
		ctx.setLocale(sendMailDto.getLang());
		ctx.setVariables(sendMailDto.getTemplateVars());

		final var mimeMessage = mailSender.createMimeMessage();

		final var message = new MimeMessageHelper(mimeMessage,
				MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

		final var subjectContent = subjectTemplateEngine.process(sendMailDto.getTemplateName(), ctx)
				.replace("\r\n", " ").replace("\n", " ").replace("    ", " ").replace("   ", " ").replace("  ", " ");
		message.setSubject(subjectContent);

		message.setFrom(sendMailDto.getSenderAddr());

		final var recipients = sendMailDto.getRecipientsAddr();
		if (recipients.size() == 1) {
			message.setTo(recipients.get(0));
		} else {
			message.setTo(recipients.toArray(new String[0]));
		}

		setCcBccRecipients(sendMailDto, message);

		final var attachedFiles = sendMailDto.getAttachedFiles();
		if (attachedFiles != null) {
			for (final var file : attachedFiles) {
				message.addAttachment(file.getName(), file);
			}
		}

		if (sendMailDto.getReplyToAddr() != null) {
			message.setReplyTo(sendMailDto.getReplyToAddr());
		}

		if (sendMailDto.getGrade() != null) {
			message.setPriority(sendMailDto.getGrade().getMessagePriority());
		}

		mimeMessage.setHeader("User-Agent", "JavaMail/MailKit");
		mimeMessage.setHeader("Content-Language", sendMailDto.getLang().getLanguage());

		setReferenceHeaders(sendMailDto, mimeMessage);

		final var htmlContent = htmlTemplateEngine.process(sendMailDto.getTemplateName(), ctx);
		message.setText(htmlContent, true);

		final var resources = sendMailDto.getResourceFiles();
		if (resources != null) {
			for (final var resource : resources) {
				final var imageSource = new ClassPathResource("static/" + resource);
				message.addInline(resource, imageSource);
			}
		}

		mailSender.send(mimeMessage);
		log.info("Send a mail to {}: \"{}\"",
				recipients.stream().collect(Collectors.joining(", ")),
				subjectContent);
		htmlTemplateEngine.clearTemplateCache();
	}

	private void setCcBccRecipients(final SendMailDto sendMailDto,
									final MimeMessageHelper message) throws MessagingException {
		final var recipientsCC = sendMailDto.getRecipientsCCAddr();
		if (recipientsCC.size() == 1) {
			message.setCc(recipientsCC.get(0));
		} else if (recipientsCC.isEmpty() == false) {
			message.setCc(recipientsCC.toArray(new String[0]));
		}

		final var recipientsBCC = sendMailDto.getRecipientsBCCAddr();
		if (recipientsBCC.size() == 1) {
			message.setBcc(recipientsBCC.get(0));
		} else if (recipientsBCC.isEmpty() == false) {
			message.setBcc(recipientsBCC.toArray(new String[0]));
		}
	}

	private void setReferenceHeaders(final SendMailDto sendMailDto,
									 final MimeMessage mimeMessage) throws MessagingException {
		if (sendMailDto.getExternalReference() != null) {
			mimeMessage.setHeader("X-ExternalReference", sendMailDto.getExternalReference());
		}

		if (sendMailDto.getSenderReference() != null) {
			mimeMessage.setHeader("X-SenderReference", sendMailDto.getSenderReference());
		}
	}

}
