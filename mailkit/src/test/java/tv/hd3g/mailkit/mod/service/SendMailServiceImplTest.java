package tv.hd3g.mailkit.mod.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.mailkit.utility.FlatJavaMailSender;

@SpringBootTest
class SendMailServiceImplTest {

	@Autowired
	FlatJavaMailSender flatMailSender;
	@Autowired
	SendMailService sendMailService;
	@Value("${mailkit.default-sender:no-reply@localhost}")
	String defaultSender;
	@Value("${mailkit.default-recipient:no-reply@localhost}")
	String defaultRecipient;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		assertFalse(MockUtil.isMock(sendMailService));
	}

	@AfterEach
	void ends() {
		flatMailSender.checkIsMessageListEmpty();
	}

	@Test
	void testSendEmail() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
				Map.of("currentdate", new Date().toString()),
				defaultSender, defaultRecipient);
		sendMailDto.setGrade(MessageGrade.TEST);

		sendMailService.sendEmail(sendMailDto);

		final var message = flatMailSender.getMessagesAndReset(1).get(0);
		message.checkHeader("User-Agent", "JavaMail/MailKit");
	}

	@Test
	void testSendEmail_Refs() throws MessagingException {
		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
				Map.of("currentdate", new Date().toString()),
				defaultSender, defaultRecipient);
		sendMailDto.setExternalReference(String.valueOf(System.nanoTime()));
		sendMailDto.setSenderReference(String.valueOf(System.nanoTime()));

		sendMailService.sendEmail(sendMailDto);

		final var message = flatMailSender.getMessagesAndReset(1).get(0);
		message.checkHeader("X-ExternalReference", sendMailDto.getExternalReference());
		message.checkHeader("X-SenderReference", sendMailDto.getSenderReference());
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
		final var message = flatMailSender.getMessagesAndReset(1).get(0);
		assertEquals(defaultSender, message.getFrom());
		assertEquals("replyto@localhost", message.getReplyTo());

		message.checkRecipients(Set.of(defaultRecipient), Set.of("cc@localhost"), Set.of("bcc@localhost"));
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
		final var message = flatMailSender.getMessagesAndReset(1).get(0);
		assertEquals(defaultSender, message.getFrom());
		assertEquals("no-reply@localhost", message.getReplyTo());

		message.checkRecipients(Set.of(
				"r1@localhost", "r2@localhost"),
				Set.of("cc1@localhost", "cc2@localhost"),
				Set.of("bcc1@localhost", "bcc2@localhost"));
	}

}
