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
import static java.util.stream.Collectors.joining;
import static org.apache.tomcat.util.http.fileupload.FileUtils.cleanDirectory;
import static org.apache.tomcat.util.http.fileupload.FileUtils.forceMkdir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class FlatJavaMailSender implements JavaMailSender {
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

	private final Function<MimeMessage, MimeMessageAnalyzer> mimeMessageAnalyzerProvider;
	private final JavaMailSender realJavaMailSender;
	private final List<MimeMessageAnalyzer> analyzers;
	private final File sendtoDir;

	public FlatJavaMailSender(final boolean sendToFile) {
		this(sendToFile, MimeMessageAnalyzer::new);
	}

	FlatJavaMailSender(final boolean sendToFile,
					   final Function<MimeMessage, MimeMessageAnalyzer> mimeMessageAnalyzerProvider) {
		this.mimeMessageAnalyzerProvider = Objects.requireNonNull(mimeMessageAnalyzerProvider,
				"\"mimeMessageAnalyzerProvider\" can't to be null");
		realJavaMailSender = new JavaMailSenderImpl();
		analyzers = new ArrayList<>();

		if (sendToFile) {
			sendtoDir = new File("target/mailkit");
			try {
				forceMkdir(sendtoDir);
				cleanDirectory(sendtoDir);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			sendtoDir = null;
		}
	}

	@Override
	public void send(final MimeMessage mimeMessage) throws MailException {
		final var mimeMessageAnalyzed = mimeMessageAnalyzerProvider.apply(mimeMessage);
		save(mimeMessageAnalyzed);
		analyzers.add(mimeMessageAnalyzed);
	}

	public List<MimeMessageAnalyzer> getMessagesAndReset(final int assertSize) {
		if (assertSize != analyzers.size()) {
			throw new IllegalArgumentException("List size=" + analyzers.size() + " is not assertSize=" + assertSize);
		}
		final var result = analyzers.stream().toList();
		analyzers.clear();
		result.forEach(this::save);
		return result;
	}

	public void checkIsMessageListEmpty() {
		if (analyzers.isEmpty()) {
			return;
		}
		throw new IllegalStateException("List is not empty (" + analyzers.size() + " items)");
	}

	private void save(final MimeMessageAnalyzer mimeMessageAnalyzer) {
		if (sendtoDir == null) {
			return;
		}

		final var outFile = new File(sendtoDir, dateFormat.format(new Date()) + ".html");
		try (final var fw = new FileWriter(outFile, UTF_8)) {
			fw.write("<!--\r\n");
			fw.write(mimeMessageAnalyzer.getHeaders().stream().collect(joining("\r\n")));
			fw.write("\r\n-->\r\n");
			fw.write(mimeMessageAnalyzer.getMailContent());
			fw.write("\r\n");
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		} catch (final MessagingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public MimeMessage createMimeMessage() {
		return realJavaMailSender.createMimeMessage();
	}

	public void clear() {
		analyzers.clear();
	}

	@Override
	public MimeMessage createMimeMessage(final InputStream contentStream) throws MailException {
		return realJavaMailSender.createMimeMessage(contentStream);
	}

	@Override
	public void send(final MimeMessage... mimeMessages) throws MailException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void send(final MimeMessagePreparator mimeMessagePreparator) throws MailException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void send(final MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void send(final SimpleMailMessage simpleMessage) throws MailException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void send(final SimpleMailMessage... simpleMessages) throws MailException {
		throw new UnsupportedOperationException();
	}

}
