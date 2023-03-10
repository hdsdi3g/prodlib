package tv.hd3g.jobkit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.jobkit.engine.JobKitEngine;

public class MockService {

	@Configuration
	@Profile({ "DefaultMock" })
	static class DefaultMock {

		@Bean
		@Primary
		SendMailService sendMailServiceMock() {
			return Mockito.mock(SendMailService.class);
		}

		@Bean
		@Primary
		ScheduledExecutorService scheduledExecutorService() {
			return Mockito.mock(ScheduledExecutorService.class);
		}

		@Bean
		@Primary
		JobKitEngine jobKitEngine() {
			return Mockito.mock(JobKitEngine.class);
		}

	}

	@Configuration
	@Profile({ "ExecFactoryMock" })
	static class ExecFactoryMock {

		@Bean
		@Primary
		ResourceBundleMessageSource resourceBundleMessageSource() {
			return new ResourceBundleMessageSource();
		}

		@Bean
		@Primary
		SendMailService sendMailServiceMock() {
			return Mockito.mock(SendMailService.class);
		}

	}

	/*
	 * =========
	 * TEST ZONE
	 * =========
	 */

	@SpringBootTest
	@ActiveProfiles({ "DefaultMock" })
	static class TestDefaultMock {
		@Autowired
		SendMailService sendMailService;
		@Autowired
		ScheduledExecutorService scheduledExecutorService;
		@Autowired
		JobKitEngine jobKitEngine;

		@Test
		void test() {
			assertTrue(MockUtil.isMock(sendMailService));
			assertTrue(MockUtil.isMock(scheduledExecutorService));
			assertTrue(MockUtil.isMock(jobKitEngine));
		}
	}

	@SpringBootTest
	@ActiveProfiles({ "ExecFactoryMock" })
	static class TestExecFactoryMock {
		@Autowired
		SendMailService sendMailService;

		@Test
		void test() {
			assertTrue(MockUtil.isMock(sendMailService));
		}
	}

}
