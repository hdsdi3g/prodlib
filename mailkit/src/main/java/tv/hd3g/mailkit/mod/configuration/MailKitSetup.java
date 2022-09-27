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

import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.messageresolver.SpringMessageResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import tv.hd3g.jobkit.engine.SupervisableManager;
import tv.hd3g.mailkit.mod.component.Translate;
import tv.hd3g.mailkit.mod.service.AppNotificationService;
import tv.hd3g.mailkit.notification.NotificationManager;
import tv.hd3g.mailkit.notification.implmail.NotificationEngineMailSetup;
import tv.hd3g.mailkit.notification.implmail.NotificationEngineMailTemplateDebug;
import tv.hd3g.mailkit.notification.implmail.NotificationEngineMailTemplateFull;
import tv.hd3g.mailkit.notification.implmail.NotificationEngineMailTemplateSimple;
import tv.hd3g.mailkit.notification.implmail.NotificationMailTemplateToolkit;
import tv.hd3g.mailkit.notification.implmail.NotificationRouterMail;

@Configuration
public class MailKitSetup {

	@Bean
	@Primary
	public ResourceBundleMessageSource resourceBundleMessageSource() {
		final var rbms = new ResourceBundleMessageSource();
		rbms.addBasenames("mailkit-messages");
		return rbms;
	}

	@Bean
	public SpringMessageResolver getSpringMessageResolver(final MessageSource rbms) {
		final var springMessageResolver = new SpringMessageResolver();
		springMessageResolver.setMessageSource(rbms);
		return springMessageResolver;
	}

	private ITemplateResolver templateResolver(final String suffix, final TemplateMode mode) {
		final var templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("/templates/");
		templateResolver.setSuffix(suffix);
		templateResolver.setTemplateMode(mode);
		templateResolver.setCharacterEncoding("UTF8");
		templateResolver.setCheckExistence(true);
		templateResolver.setCacheable(false);
		return templateResolver;
	}

	@Bean(name = "htmlTemplateEngine")
	public TemplateEngine htmlTemplateEngine(final SpringMessageResolver springMessageResolver) {
		final var templateEngine = new TemplateEngine();
		templateEngine.addTemplateResolver(templateResolver(".html", TemplateMode.HTML));
		templateEngine.addMessageResolver(springMessageResolver);
		return templateEngine;
	}

	@Bean(name = "subjectTemplateEngine")
	public TemplateEngine subjectTemplateEngine(final SpringMessageResolver springMessageResolver) {
		final var templateEngine = new TemplateEngine();
		templateEngine.addTemplateResolver(templateResolver(".txt", TemplateMode.TEXT));
		templateEngine.addMessageResolver(springMessageResolver);
		return templateEngine;
	}

	@Bean
	public NotificationManager getNotificationManager(final ResourceBundleMessageSource rbms,
													  final AppNotificationService appNotificationService,
													  final MailKitConfig config,
													  final JavaMailSender mailSender,
													  final Translate translate,
													  final SupervisableManager supervisableManager) {
		Optional.ofNullable(appNotificationService.getMessageSourceBasename())
				.ifPresent(rbms::addBasenames);
		final var toolkit = new NotificationMailTemplateToolkit(translate, config.getEnv());

		final var setupEngine = new NotificationEngineMailSetup(
				supervisableManager,
				appNotificationService,
				mailSender,
				config.getSenderAddr(),
				Optional.ofNullable(config.getReplyToAddr()).orElse(config.getSenderAddr()),
				config.getGroupDev(),
				config.getGroupAdmin(),
				config.getGroupSecurity());

		final var router = new NotificationRouterMail(
				new NotificationEngineMailTemplateSimple(toolkit),
				new NotificationEngineMailTemplateFull(toolkit),
				new NotificationEngineMailTemplateDebug(toolkit),
				setupEngine);

		return new NotificationManager().register(router).register(supervisableManager);
	}

}
