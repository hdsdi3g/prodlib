package tv.hd3g.mailkit.mod.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.commons.mailkit.SendMailService;

@SpringBootTest
@ActiveProfiles({ "realSendMailService" })
@TestPropertySource(properties = {
                                   "mailkit.sendtoFile=target/mailkit",
                                   "mailkit.sendtoFileWipeOnStart=true",
                                   "mailkit.sendtoFileAutomaticExt=true"
})
class SendMailToFileServiceTest {

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

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		assertNotNull(sendtoFile);
		assertTrue(wipeOnStart);

		assertTrue(MockUtil.isMock(mailSender));
		assertFalse(MockUtil.isMock(sendMailService));
		when(mailSender.createMimeMessage()).thenReturn(
		        new MimeMessage(Session.getDefaultInstance(new Properties())));
	}

	@Test
	void testWrite() throws MessagingException {
		assertEquals(0, sendtoFile.list().length);

		final var sendMailDto = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender, defaultRecipient);
		sendMailDto.setGrade(MessageGrade.TEST);
		sendMailService.sendEmail(sendMailDto);

		assertEquals(2, sendtoFile.list().length);
		final var dropped = List.of(sendtoFile.list());
		assertTrue(dropped.stream().anyMatch(f -> f.endsWith(".html")));
		assertTrue(dropped.stream().anyMatch(f -> f.endsWith(".txt")));
	}
}
