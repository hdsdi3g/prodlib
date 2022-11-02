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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jakarta.mail.Address;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import net.datafaker.Faker;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;

class MimeMessageAnalyzerTest {
	static Faker faker = Faker.instance();

	MimeMessageAnalyzer a;
	String messageStr;
	String name;
	String value;
	Header header;
	Address addrValue;

	@Mock
	MimeMessage message;
	@Mock
	MimeMultipart mime;
	@Mock
	MimeMultipart mime0;
	@Mock
	MimeBodyPart bodyPart;
	@Mock
	MimeBodyPart bodyPart0;
	@Mock
	Enumeration<Header> headers;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		a = new MimeMessageAnalyzer(message);
		messageStr = faker.numerify("message###");
		header = new Header(faker.numerify("name###"), faker.numerify("value###"));
		name = faker.numerify("name###");
		value = faker.numerify("value###");

		addrValue = makeAddress(value);
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(message, mime, bodyPart, mime0, bodyPart0);
	}

	Address makeAddress(final String addr) {
		return new Address() {

			@Override
			public String toString() {
				return addr;
			}

			@Override
			public String getType() {
				return null;
			}

			@Override
			public boolean equals(final Object address) {
				return false;
			}
		};
	}

	Address makeRandomAddress() {
		return makeAddress(faker.numerify("address###"));
	}

	@Test
	void testGetMailContent() throws MessagingException, IOException {
		when(message.getContent()).thenReturn(mime);
		when(mime.getCount()).thenReturn(1);
		when(mime.getBodyPart(0)).thenReturn(bodyPart);
		when(bodyPart.getContent()).thenReturn(mime0);
		when(bodyPart.getContentType()).thenReturn("");
		when(mime0.getCount()).thenReturn(1);
		when(mime0.getBodyPart(0)).thenReturn(bodyPart0);
		when(bodyPart0.getContent()).thenReturn(messageStr);
		when(bodyPart0.getContentType()).thenReturn("");

		assertEquals(messageStr, a.getMailContent());

		verify(message, atLeastOnce()).getContent();
		verify(mime, atLeastOnce()).getBodyPart(0);
		verify(mime, atLeastOnce()).getCount();
		verify(bodyPart, atLeastOnce()).getContent();
		verify(bodyPart, atLeastOnce()).getContentType();
		verify(mime0, atLeastOnce()).getBodyPart(0);
		verify(mime0, atLeastOnce()).getCount();
		verify(bodyPart0, atLeastOnce()).getContent();
		verify(bodyPart0, atLeastOnce()).getContentType();
	}

	@Test
	void testGetMailContent_IOError() throws MessagingException, IOException {
		when(message.getContent()).thenThrow(IOException.class);
		assertThrows(UncheckedIOException.class, () -> a.getMailContent());
		verify(message, times(1)).getContent();
	}

	@Test
	void testGetMailContent_typeError() throws MessagingException, IOException {
		when(message.getContent()).thenReturn(new Object());
		assertThrows(IllegalArgumentException.class, () -> a.getMailContent());
		verify(message, times(2)).getContent();
	}

	@Test
	void testPrintFullEmail() throws MessagingException, IOException {
		a.printFullEmail();
		verify(message, times(1)).writeTo(System.out);
	}

	@Test
	void testPrintFullEmail_Error() throws MessagingException, IOException {
		doThrow(IOException.class).when(message).writeTo(System.out);
		assertThrows(UncheckedIOException.class, () -> a.printFullEmail());
		verify(message, times(1)).writeTo(System.out);
	}

	@Test
	void testGetHeaders() throws MessagingException {
		when(message.getAllHeaders()).thenReturn(headers);
		when(headers.asIterator()).thenReturn(Set.of(header).iterator());

		assertEquals(List.of(header.getName() + ": " + header.getValue()), a.getHeaders());
		verify(message, times(1)).getAllHeaders();
		verify(headers, times(1)).asIterator();
	}

	@Test
	void testGetHeader() throws MessagingException {
		when(message.getHeader(name, "")).thenReturn(value);
		assertEquals(value, a.getHeader(name));
		verify(message, times(1)).getHeader(name, "");
	}

	@Test
	void testGetHeadersString() throws MessagingException {
		when(message.getHeader(name)).thenReturn(new String[] { value });
		assertEquals(List.of(value), a.getHeaders(name));
		verify(message, times(1)).getHeader(name);
	}

	@Test
	void testGetFrom() throws MessagingException {
		when(message.getFrom()).thenReturn(new Address[] { addrValue, makeRandomAddress(), makeRandomAddress() });
		assertEquals(value, a.getFrom());
		verify(message, times(1)).getFrom();
	}

	@Test
	void testGetReplyTo() throws MessagingException {
		when(message.getReplyTo()).thenReturn(new Address[] { addrValue, makeRandomAddress(), makeRandomAddress() });
		assertEquals(value, a.getReplyTo());
		verify(message, times(1)).getReplyTo();
	}

	@Test
	void testGetRecipients() throws MessagingException {
		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { addrValue });
		assertEquals(Set.of(value), a.getRecipients());
		verify(message, times(1)).getRecipients(Message.RecipientType.TO);
	}

	@Test
	void testGetRecipientsCC() throws MessagingException {
		when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { addrValue });
		assertEquals(Set.of(value), a.getRecipientsCC());
		verify(message, times(1)).getRecipients(Message.RecipientType.CC);
	}

	@Test
	void testGetRecipientsBCC() throws MessagingException {
		when(message.getRecipients(Message.RecipientType.BCC)).thenReturn(new Address[] { addrValue });
		assertEquals(Set.of(value), a.getRecipientsBCC());
		verify(message, times(1)).getRecipients(Message.RecipientType.BCC);
	}

	@Test
	void testCheckRecipients_ok() throws MessagingException {
		final var addrTo = makeAddress(faker.numerify("addrTo###"));
		final var addrCC = makeAddress(faker.numerify("addrCC###"));
		final var addrBCC = makeAddress(faker.numerify("addrBCC###"));

		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { addrTo });
		when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { addrCC });
		when(message.getRecipients(Message.RecipientType.BCC)).thenReturn(new Address[] { addrBCC });

		a.checkRecipients(Set.of(addrTo.toString()), Set.of(addrCC.toString()), Set.of(addrBCC.toString()));

		verify(message, times(1)).getRecipients(Message.RecipientType.TO);
		verify(message, times(1)).getRecipients(Message.RecipientType.CC);
		verify(message, times(1)).getRecipients(Message.RecipientType.BCC);
	}

	@Test
	void testCheckRecipients_badA() throws MessagingException {
		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { makeRandomAddress() });

		assertThrows(IllegalArgumentException.class, // NOSONAR S5778
				() -> a.checkRecipients(Set.of(""), Set.of(), Set.of()));

		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.TO);
	}

	@Test
	void testCheckRecipients_badCC() throws MessagingException {
		final var addrTo = makeAddress(faker.numerify("addrTo###"));
		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { addrTo });
		when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { makeRandomAddress() });

		assertThrows(IllegalArgumentException.class, // NOSONAR S5778
				() -> a.checkRecipients(Set.of(addrTo.toString()), Set.of(""), Set.of()));

		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.TO);
		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.CC);
	}

	@Test
	void testCheckRecipients_badBCC() throws MessagingException {
		final var addrTo = makeAddress(faker.numerify("addrTo###"));
		final var addrCC = makeAddress(faker.numerify("addrCC###"));
		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { addrTo });
		when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { addrCC });
		when(message.getRecipients(Message.RecipientType.BCC)).thenReturn(new Address[] { makeRandomAddress() });

		assertThrows(IllegalArgumentException.class, // NOSONAR S5778
				() -> a.checkRecipients(Set.of(addrTo.toString()), Set.of(addrCC.toString()), Set.of()));

		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.TO);
		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.CC);
		verify(message, atLeastOnce()).getRecipients(Message.RecipientType.BCC);
	}

	@Test
	void testCheckHeader_ok() throws MessagingException {
		when(message.getHeader(name, "")).thenReturn(value);
		a.checkHeader(name, value);
		verify(message, times(1)).getHeader(name, "");
	}

	@Test
	void testCheckHeader_invalid() throws MessagingException {
		when(message.getHeader(name, "")).thenReturn(faker.numerify("badHeader###"));
		assertThrows(IllegalArgumentException.class, () -> a.checkHeader(name, value));
		verify(message, times(1)).getHeader(name, "");
	}

	@Test
	void testCheckHeaders() throws MessagingException {
		final var senderAddr = faker.numerify("senderAddr###");
		final var replyToAddr = faker.numerify("replyToAddr###");
		final var recipientsAddr = faker.numerify("recipientsAddr###");
		final var lang = faker.options().option(Locale.getAvailableLocales());
		final var userAgent = faker.numerify("userAgent###");
		final var grade = faker.options().option(MessageGrade.class);

		when(message.getFrom()).thenReturn(new Address[] { makeAddress(senderAddr) });
		when(message.getReplyTo()).thenReturn(new Address[] { makeAddress(replyToAddr) });
		when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { makeAddress(recipientsAddr) });
		when(message.getHeader("Content-Language", "")).thenReturn(lang.toString());
		when(message.getHeader("User-Agent", "")).thenReturn(userAgent);
		when(message.getHeader("X-Priority", "")).thenReturn(String.valueOf(grade.getMessagePriority()));

		a.checkHeaders(senderAddr, replyToAddr, Set.of(recipientsAddr), lang, userAgent, grade);

		verify(message, times(1)).getFrom();
		verify(message, times(1)).getReplyTo();
		verify(message, times(1)).getRecipients(Message.RecipientType.TO);
		verify(message, times(1)).getHeader("Content-Language", "");
		verify(message, times(1)).getHeader("User-Agent", "");
		verify(message, times(1)).getHeader("X-Priority", "");
	}

	@Test
	void testCheckHeaders_badHeader() throws MessagingException {
		when(message.getFrom()).thenReturn(new Address[] { makeRandomAddress() });
		assertThrows(IllegalArgumentException.class, () -> a.checkHeaders("", "", Set.of(""), null, null, null)); // NOSONAR S5778
		verify(message, times(1)).getFrom();
	}

}
