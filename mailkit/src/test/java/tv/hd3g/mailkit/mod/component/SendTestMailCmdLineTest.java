package tv.hd3g.mailkit.mod.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.commons.mailkit.SendMailService;

@SpringBootTest
class SendTestMailCmdLineTest {

	@MockBean
	SendMailService sendMailService;
	@Autowired
	SendTestMailCmdLine sendTestMailCmdLine;

	@Mock
	private ApplicationArguments args;

	private ArgumentCaptor<SendMailDto> emailCaptor;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		/**
		 * Mail mock preparation
		 */
		assertTrue(MockUtil.isMock(sendMailService));
		emailCaptor = ArgumentCaptor.forClass(SendMailDto.class);
	}

	@Test
	void testRun() throws Exception {
		final var parameters = List.of("send-test-mail", "dont-quit-after-done");
		when(args.getNonOptionArgs()).thenReturn(parameters);

		sendTestMailCmdLine.run(args);

		Mockito.verify(sendMailService, Mockito.only()).sendEmail(emailCaptor.capture());
		final var mailDto = emailCaptor.getValue();
		assertNotNull(mailDto);
		assertEquals(MessageGrade.TEST, mailDto.getGrade());
	}

}
