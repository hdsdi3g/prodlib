/*
 * This file is part of MailKit.
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
package tv.hd3g.mailkit.mod.component;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailDto.MessageGrade;
import tv.hd3g.commons.mailkit.SendMailService;

@Component
public class SendTestMailCmdLine implements ApplicationRunner {

	@Autowired
	private SendMailService sendMailService;
	@Value("${mailkit.default-sender:no-reply@localhost}")
	private String defaultSender;
	@Value("${mailkit.default-recipient:no-reply@localhost}")
	private String defaultRecipient;

	@Override
	public void run(final ApplicationArguments args) throws Exception {
		if (args.getNonOptionArgs().contains("send-test-mail") == false) {
			return;
		}
		final var mail = new SendMailDto("internal-mail-test", Locale.getDefault(),
		        Map.of("currentdate", new Date().toString()),
		        defaultSender, defaultRecipient);
		mail.setGrade(MessageGrade.TEST);
		sendMailService.sendEmail(mail);

		if (args.getNonOptionArgs().contains("dont-quit-after-done") == false) {
			System.exit(0);
		}
	}

}
