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
package tv.hd3g.mailkit.mod.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.messageresolver.SpringMessageResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class MailKitSetup {

	private final SpringMessageResolver springMessageResolver;

	@Autowired
	public MailKitSetup(final ResourceBundleMessageSource rbms, final MessageSource messageSource) {
		rbms.addBasenames("mailkit-messages");
		springMessageResolver = new SpringMessageResolver();
		springMessageResolver.setMessageSource(messageSource);
	}

	private ITemplateResolver templateResolver(final String suffix, final TemplateMode mode) {
		final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("/templates/");
		templateResolver.setSuffix(suffix);
		templateResolver.setTemplateMode(mode);
		templateResolver.setCharacterEncoding("UTF8");
		templateResolver.setCheckExistence(true);
		templateResolver.setCacheable(false);
		return templateResolver;
	}

	@Bean(name = "htmlTemplateEngine")
	public TemplateEngine htmlTemplateEngine() {
		final TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.addTemplateResolver(templateResolver(".html", TemplateMode.HTML));
		templateEngine.addMessageResolver(springMessageResolver);
		return templateEngine;
	}

	@Bean(name = "subjectTemplateEngine")
	public TemplateEngine subjectTemplateEngine() {
		final TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.addTemplateResolver(templateResolver(".txt", TemplateMode.TEXT));
		templateEngine.addMessageResolver(springMessageResolver);
		return templateEngine;
	}

}
