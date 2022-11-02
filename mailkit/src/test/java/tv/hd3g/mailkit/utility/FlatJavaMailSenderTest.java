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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.internal.util.MockUtil;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import net.datafaker.Faker;

class FlatJavaMailSenderTest {
	static Faker faker = Faker.instance();

	@Mock
	MimeMessage mimeMessage;
	@Mock
	MimeMessageAnalyzer mimeMessageAnalyzer;
	@Mock
	Function<MimeMessage, MimeMessageAnalyzer> mimeMessageAnalyzerProvider;
	@Mock
	InputStream contentStream;
	@Mock
	MimeMessagePreparator mimeMessagePreparator;
	@Mock
	SimpleMailMessage simpleMailMessage;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		// assertTrue(MockUtil.isMock());
		// MockUtil.resetMock(toolRunner);
		// @MockBean
		// @Captor ArgumentCaptor<>
		// Mockito.doThrow(new Exception()).when();
		when(mimeMessageAnalyzerProvider.apply(mimeMessage)).thenReturn(mimeMessageAnalyzer);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(mimeMessage, mimeMessageAnalyzer, contentStream);
	}

	@Nested
	class Save {
		FlatJavaMailSender f;
		String header;
		String mailContent;

		@BeforeEach
		void init() throws Exception {
			f = new FlatJavaMailSender(true, mimeMessageAnalyzerProvider);
			header = faker.numerify("header###");
			mailContent = faker.numerify("mailContent###");
			when(mimeMessageAnalyzer.getHeaders()).thenReturn(List.of(header));
			when(mimeMessageAnalyzer.getMailContent()).thenReturn(mailContent);
		}

		@Test
		void testSendMimeMessage() throws MessagingException, IOException {
			f.send(mimeMessage);
			verify(mimeMessageAnalyzerProvider, times(1)).apply(mimeMessage);
			verify(mimeMessageAnalyzer, times(1)).getHeaders();
			verify(mimeMessageAnalyzer, times(1)).getMailContent();

			final var outFiles = List.of(new File("target/mailkit")
					.listFiles(f -> f.getName().endsWith(".html")));
			assertEquals(1, outFiles.size());
			final var lines = Files.readAllLines(outFiles.get(0).toPath());
			lines.stream().anyMatch(l -> l.equals(header));
			lines.stream().anyMatch(l -> l.equals(mailContent));
		}
	}

	@Test
	void testSendMimeMessage_noSave() {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		f.send(mimeMessage);
		verify(mimeMessageAnalyzerProvider, times(1)).apply(mimeMessage);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testGetMessagesAndReset_empty(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertEquals(List.of(), f.getMessagesAndReset(0));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testGetMessagesAndReset(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		f.send(mimeMessage);
		assertEquals(List.of(mimeMessageAnalyzer), f.getMessagesAndReset(1));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testGetMessagesAndReset_2(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		f.send(mimeMessage);
		f.send(mimeMessage);
		assertEquals(List.of(mimeMessageAnalyzer, mimeMessageAnalyzer), f.getMessagesAndReset(2));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testGetMessagesAndReset_badSize(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(IllegalArgumentException.class, () -> f.getMessagesAndReset(1));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCheckIsMessageListEmpty(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		f.checkIsMessageListEmpty();
		f.send(mimeMessage);
		assertThrows(IllegalStateException.class, () -> f.checkIsMessageListEmpty());
		f.getMessagesAndReset(1);
		f.checkIsMessageListEmpty();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testClear(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		f.send(mimeMessage);
		f.clear();
		f.checkIsMessageListEmpty();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCreateMimeMessage(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertNotNull(f.createMimeMessage());
		assertFalse(MockUtil.isMock(f.createMimeMessage()));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCreateMimeMessageInputStream(final boolean save) throws IOException {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertNotNull(f.createMimeMessage(contentStream));
		assertFalse(MockUtil.isMock(f.createMimeMessage(contentStream)));
		verify(contentStream, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSendMimeMessageArray(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(UnsupportedOperationException.class,
				() -> f.send(new MimeMessage[] { mimeMessage }));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSendMimeMessagePreparator(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(UnsupportedOperationException.class,
				() -> f.send(mimeMessagePreparator));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSendMimeMessagePreparatorArray(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(UnsupportedOperationException.class,
				() -> f.send(new MimeMessagePreparator[] { mimeMessagePreparator }));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSendSimpleMailMessage(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(UnsupportedOperationException.class, () -> f.send(simpleMailMessage));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSendSimpleMailMessageArray(final boolean save) {
		final var f = new FlatJavaMailSender(false, mimeMessageAnalyzerProvider);
		assertThrows(UnsupportedOperationException.class,
				() -> f.send(new SimpleMailMessage[] { simpleMailMessage }));
	}

}
