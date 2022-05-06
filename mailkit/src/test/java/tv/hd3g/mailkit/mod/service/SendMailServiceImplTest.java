package tv.hd3g.mailkit.mod.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.commons.mailkit.SendMailService;

@SpringBootTest
@ActiveProfiles({ "realSendMailService" })
class SendMailServiceImplTest {

	@Autowired
	JavaMailSender mailSender;
	@Autowired
	SendMailService sendMailService;
	@Value("${mailkit.default-sender:no-reply@localhost}")
	private String defaultSender;
	@Value("${mailkit.default-recipient:no-reply@localhost}")
	private String defaultRecipient;
	@Value("${mailkit.sendtoFile:#{null}}")
	File sendtoFile;
	@Value("${mailkit.sendtoFileWipeOnStart:false}")
	boolean wipeOnStart;

	@Captor
	ArgumentCaptor<MimeMessage> mimeMessageCaptor;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		assertTrue(MockUtil.isMock(mailSender));
		assertFalse(MockUtil.isMock(sendMailService));

		when(mailSender.createMimeMessage()).thenReturn(
		        new MimeMessage(Session.getDefaultInstance(new Properties())));
		mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

		assertNull(sendtoFile);
		assertFalse(wipeOnStart);
	}

	private static List<Header> getHeaders(final MimeMessage message) throws MessagingException {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
		        message.getAllHeaders().asIterator(), Spliterator.ORDERED), false)
		        .collect(Collectors.toUnmodifiableList());
	}

	@Test
	void testSendEmail() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender, defaultRecipient);
		sendMailDto.setGrade(MessageGrade.TEST);

		sendMailService.sendEmail(sendMailDto);
		Mockito.verify(mailSender, Mockito.atLeast(1)).send(mimeMessageCaptor.capture());

		final var message = mimeMessageCaptor.getValue();
		assertNotNull(message);

		final var ua = getHeaders(message).stream()
		        .filter(h -> h.getName().equalsIgnoreCase("User-Agent"))
		        .findFirst()
		        .orElseThrow(() -> new IllegalArgumentException("Can't find User-Agent in mail headers"));
		assertEquals("JavaMail/MailKit", ua.getValue());
	}

	@Test
	void testSendEmail_Refs() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender, defaultRecipient);
		sendMailDto.setExternalReference(String.valueOf(System.nanoTime()));
		sendMailDto.setSenderReference(String.valueOf(System.nanoTime()));

		sendMailService.sendEmail(sendMailDto);
		Mockito.verify(mailSender, Mockito.atLeast(1)).send(mimeMessageCaptor.capture());

		final var message = mimeMessageCaptor.getValue();
		assertNotNull(message);

		final var headers = getHeaders(message);
		assertEquals(sendMailDto.getExternalReference(),
		        headers.stream()
		                .filter(h -> h.getName().equalsIgnoreCase("X-ExternalReference"))
		                .findFirst()
		                .orElseThrow(() -> new IllegalArgumentException(
		                        "Can't find X-ExternalReference in mail headers"))
		                .getValue());

		assertEquals(sendMailDto.getSenderReference(),
		        headers.stream()
		                .filter(h -> h.getName().equalsIgnoreCase("X-SenderReference"))
		                .findFirst()
		                .orElseThrow(() -> new IllegalArgumentException("Can't find X-SenderReference in mail headers"))
		                .getValue());
	}

	@Test
	void testSendEmail_CcBcc() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender,
		        List.of(defaultRecipient),
		        List.of("cc@localhost"),
		        List.of("bcc@localhost"));
		sendMailDto.setReplyToAddr("replyto@localhost");

		sendMailService.sendEmail(sendMailDto);
		Mockito.verify(mailSender, Mockito.atLeast(1)).send(mimeMessageCaptor.capture());

		final var message = mimeMessageCaptor.getValue();
		assertNotNull(message);

		final var from = List.of(message.getFrom());
		assertNotNull(from);
		assertEquals(1, from.size());
		assertEquals(defaultSender, ((InternetAddress) from.get(0)).getAddress());

		final var replyTo = List.of(message.getReplyTo());
		assertNotNull(replyTo);
		assertEquals(1, replyTo.size());
		assertEquals("replyto@localhost", ((InternetAddress) replyTo.get(0)).getAddress());

		final var recipients = List.of(message.getRecipients(Message.RecipientType.TO));
		assertNotNull(recipients);
		assertEquals(1, recipients.size());
		assertEquals(defaultRecipient, ((InternetAddress) recipients.get(0)).getAddress());

		final var cc = List.of(message.getRecipients(Message.RecipientType.CC));
		assertNotNull(cc);
		assertEquals(1, cc.size());
		assertEquals("cc@localhost", ((InternetAddress) cc.get(0)).getAddress());

		final var bcc = List.of(message.getRecipients(Message.RecipientType.BCC));
		assertNotNull(bcc);
		assertEquals(1, bcc.size());
		assertEquals("bcc@localhost", ((InternetAddress) bcc.get(0)).getAddress());
	}

	@Test
	void testSendEmail_MultipleCcBcc() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender,
		        List.of("r1@localhost", "r2@localhost"),
		        List.of("cc1@localhost", "cc2@localhost"),
		        List.of("bcc1@localhost", "bcc2@localhost"));

		sendMailService.sendEmail(sendMailDto);
		Mockito.verify(mailSender, Mockito.atLeast(1)).send(mimeMessageCaptor.capture());

		final var message = mimeMessageCaptor.getValue();
		assertNotNull(message);

		final var recipients = List.of(message.getRecipients(Message.RecipientType.TO));
		assertNotNull(recipients);
		assertEquals(2, recipients.size());
		assertEquals("r1@localhost", ((InternetAddress) recipients.get(0)).getAddress());
		assertEquals("r2@localhost", ((InternetAddress) recipients.get(1)).getAddress());

		final var cc = List.of(message.getRecipients(Message.RecipientType.CC));
		assertNotNull(cc);
		assertEquals(2, cc.size());
		assertEquals("cc1@localhost", ((InternetAddress) cc.get(0)).getAddress());
		assertEquals("cc2@localhost", ((InternetAddress) cc.get(1)).getAddress());

		final var bcc = List.of(message.getRecipients(Message.RecipientType.BCC));
		assertNotNull(bcc);
		assertEquals(2, bcc.size());
		assertEquals("bcc1@localhost", ((InternetAddress) bcc.get(0)).getAddress());
		assertEquals("bcc2@localhost", ((InternetAddress) bcc.get(1)).getAddress());
	}

}
