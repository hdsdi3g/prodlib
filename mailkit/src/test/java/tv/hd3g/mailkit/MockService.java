package tv.hd3g.mailkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.commons.mailkit.SendMailService;

public class MockService {

	@Configuration
	@Profile({ "mockSendMailService" })
	static class MockSendMailService {

		@Bean
		public SendMailService sendMailService() {
			return Mockito.mock(SendMailService.class);
		}
	}

	@Configuration
	@Profile({ "realSendMailService" }) // "!test",
	static class RealSendMailService {

		@Autowired
		SendMailService realSendMailService;

		@Bean
		public SendMailService sendMailService() {
			return realSendMailService;
		}
	}

	@Configuration
	static class MockJavaMailSender {

		@Bean
		public JavaMailSender javaMailSender() {
			return Mockito.mock(JavaMailSender.class);
		}
	}

	/*
	 * =========
	 * TEST ZONE
	 * =========
	 */

	@SpringBootTest
	@ActiveProfiles({ "mockSendMailService" })
	static class TestRealSendMailService {
		@Autowired
		SendMailService sendMailService;

		@Test
		void test() {
			assertTrue(MockUtil.isMock(sendMailService));
		}
	}

	@SpringBootTest
	@ActiveProfiles({ "realSendMailService" })
	static class TestMockSendMailService {
		@Autowired
		SendMailService sendMailService;

		@Test
		void test() {
			assertFalse(MockUtil.isMock(sendMailService));
		}
	}

	@SpringBootTest
	static class TestMockJavaMailSender {
		@Autowired
		JavaMailSender javaMailSender;

		@Test
		void test() {
			assertTrue(MockUtil.isMock(javaMailSender));
		}
	}

}
