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
package tv.hd3g.mailkit.utility;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.StreamSupport.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;

public class MimeMessageAnalyzer {
	private final MimeMessage message;

	public MimeMessageAnalyzer(final MimeMessage message) {
		this.message = Objects.requireNonNull(message, "\"message\" can't to be null");
	}

	private String getBodyPartContent(final BodyPart bodyPart) {
		try {
			if (bodyPart.getContentType().equalsIgnoreCase("message/delivery-status")) {
				return "message/delivery-status";
			}
			if (bodyPart.getContent() instanceof final MimeMultipart mimeMultipart) {
				return getBodyParts(mimeMultipart).stream()
						.map(this::getBodyPartContent)
						.collect(Collectors.joining(System.lineSeparator()));
			} else if (bodyPart.getContent() instanceof final MimeMessage mimeMessage) {
				return (String) mimeMessage.getContent();
			} else if (bodyPart.getContent() instanceof final String strPart) {
				return strPart;
			} else if (bodyPart.getContent() instanceof final SharedByteArrayInputStream is) {
				if ("Content-Type: text/plain; charset=utf-8".equals(bodyPart.getContentType()) == false) {
					throw new IllegalArgumentException("Unknow body part: " + bodyPart.getContent().getClass().getName()
													   + ", " + bodyPart.getContentType());
				}
				final var bias = new ByteArrayOutputStream();
				IOUtils.copy(is, bias);
				return new String(bias.toByteArray(), UTF_8);
			} else {
				throw new IllegalArgumentException("Unknow body part: " + bodyPart.getContent().getClass().getName()
												   + ", " + bodyPart.getContentType());
			}
		} catch (IOException | MessagingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private List<BodyPart> getBodyParts(final MimeMultipart mimeMultipart) throws MessagingException {
		final var list = new ArrayList<BodyPart>();
		for (var pos = 0; pos < mimeMultipart.getCount(); pos++) {
			list.add(mimeMultipart.getBodyPart(pos));
		}
		return list;
	}

	public String getMailContent() throws MessagingException {
		try {
			if (message.getContent() instanceof final MimeMultipart multipartContent) {
				return getBodyParts(multipartContent).stream()
						.map(this::getBodyPartContent)
						.collect(Collectors.joining(System.lineSeparator()));
			} else {
				throw new IllegalArgumentException("Can't manage content " + message.getContent().getClass());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void printFullEmail() throws MessagingException {
		try {
			message.writeTo(System.out);// NOSONAR S106
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public List<String> getHeaders() throws MessagingException {
		return stream(spliteratorUnknownSize(message.getAllHeaders().asIterator(), ORDERED), false)
				.map(h -> h.getName() + ": " + h.getValue())
				.toList();
	}

	public String getHeader(final String name) throws MessagingException {
		return message.getHeader(name, "");
	}

	public List<String> getHeaders(final String name) throws MessagingException {
		return List.of(message.getHeader(name));
	}

	private String makeExceptionMessage(final Object expected, final Object actual, final String what) {
		return "Invalid " + what + " expected: " + expected + ", actual: " + actual;
	}

	private void checkError(final Object expected, final Object actual, final String what) {
		if (Objects.equals(expected, actual) == false) {
			throw new IllegalArgumentException(makeExceptionMessage(expected, actual, what));
		}
	}

	public String getFrom() throws MessagingException {
		return Stream.of(message.getFrom())
				.map(Object::toString)
				.findFirst()
				.orElse(null);
	}

	public String getReplyTo() throws MessagingException {
		return Stream.of(message.getReplyTo())
				.map(Object::toString)
				.findFirst()
				.orElse(null);
	}

	private Set<String> getRecipients(final Message.RecipientType type) throws MessagingException {
		return Optional.ofNullable(message.getRecipients(type))
				.map(List::of)
				.stream()
				.flatMap(List::stream)
				.map(Object::toString)
				.map(r -> r.replace("\"", ""))
				.collect(toUnmodifiableSet());
	}

	public Set<String> getRecipients() throws MessagingException {
		return getRecipients(Message.RecipientType.TO);
	}

	public Set<String> getRecipientsCC() throws MessagingException {
		return getRecipients(Message.RecipientType.CC);
	}

	public Set<String> getRecipientsBCC() throws MessagingException {
		return getRecipients(Message.RecipientType.BCC);
	}

	public void checkRecipients(final Set<String> to,
								final Set<String> cc,
								final Set<String> bcc) throws MessagingException {
		if (to.equals(getRecipients()) == false) {
			throw new IllegalArgumentException(makeExceptionMessage(to, getRecipients(), "recipients To"));
		}
		if (cc.equals(getRecipientsCC()) == false) {
			throw new IllegalArgumentException(makeExceptionMessage(cc, getRecipientsCC(), "recipients CC"));
		}
		if (bcc.equals(getRecipientsBCC()) == false) {
			throw new IllegalArgumentException(makeExceptionMessage(bcc, getRecipientsBCC(), "recipients BCC"));
		}
	}

	public void checkHeaders(final String senderAddr,
							 final String replyToAddr,
							 final Set<String> recipientsAddr,
							 final Locale lang,
							 final String userAgent,
							 final MessageGrade grade) throws MessagingException {
		checkError(senderAddr, getFrom(), "sender");
		checkError(replyToAddr, getReplyTo(), "replyTo");
		checkError(recipientsAddr, getRecipients(), "recipients");
		checkHeader("Content-Language", lang.toString());
		checkHeader("User-Agent", userAgent);
		checkError(String.valueOf(grade.getMessagePriority()), getHeader("X-Priority"), "Priority");
	}

	public void checkHeader(final String name, final Object expected) throws MessagingException {
		checkError(expected, getHeader(name), name);
	}

}
