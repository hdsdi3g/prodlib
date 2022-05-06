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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.mailkit.mod.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
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

import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.mailkit.mod.service.AppLifeCycleMailService;

@SpringBootTest
@ActiveProfiles({ "realSendMailService" })
@TestPropertySource(properties = {
                                   "mailkit.sendtoFile=target/mailkit",
                                   "mailkit.sendtoFileWipeOnStart=true"
})
class AppLifeCycleMailServiceAbstractTest {

	@Autowired
	AppLifeCycleMailService appLifeCycleMail;
	@Autowired
	JavaMailSender mailSender;
	@Autowired
	SendMailService sendMailService;
	@Value("${mailkit.sendtoFile:#{null}}")
	File sendtoFile;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		assertNotNull(sendtoFile);
		FileUtils.cleanDirectory(sendtoFile);

		assertTrue(MockUtil.isMock(mailSender));
		assertFalse(MockUtil.isMock(sendMailService));
		when(mailSender.createMimeMessage()).thenReturn(
		        new MimeMessage(Session.getDefaultInstance(new Properties())));
	}

	@Test
	void testOnAppStartupService() {
		appLifeCycleMail.onAppStartupService("MyService");
		assertEquals(2, sendtoFile.list().length);
	}

	@Test
	void testOnAppStopService() {
		appLifeCycleMail.onAppStopService("MyService");
		assertEquals(2, sendtoFile.list().length);
	}

	@Test
	void testOnAppGenericError() {
		try {
			throw new Exception("Some simple error");
		} catch (final Exception e) {
			appLifeCycleMail.onAppGenericError("MyService", "Error cause", e);
		}
		assertEquals(2, sendtoFile.list().length);
	}

	@Test
	void testOnAppCantStartupService() {
		try {
			throw new Exception("Some simple error");
		} catch (final Exception e) {
			appLifeCycleMail.onAppCantStartupService("MyService", "Error cause", e);
		}
		assertEquals(2, sendtoFile.list().length);
	}

}
