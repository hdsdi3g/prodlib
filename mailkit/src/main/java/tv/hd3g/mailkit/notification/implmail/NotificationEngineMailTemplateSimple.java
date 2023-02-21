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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.mailkit.notification.implmail;

import static j2html.TagCreator.hr;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import j2html.tags.DomContent;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;

public class NotificationEngineMailTemplateSimple implements NotificationMailMessageProducer {

	private final NotificationMailTemplateToolkit toolkit;

	public NotificationEngineMailTemplateSimple(final NotificationMailTemplateToolkit toolkit) {
		this.toolkit = Objects.requireNonNull(toolkit, "\"toolkit\" can't to be null");
	}

	@Override
	public NotificationMailMessage makeMessage(final NotificationMailMessageProducerEnvironment env,
											   final SupervisableEndEvent event) {
		return new NotificationMailMessage(
				toolkit.processSubject(env.lang(), event),
				toolkit.processHTMLMessage(assembleHTMLMessageBodyContent(env, event)));
	}

	private HtmlCssDocumentPayload assembleHTMLMessageBodyContent(final NotificationMailMessageProducerEnvironment env,
																  final SupervisableEndEvent event) {
		final var listBodyContent = new ArrayList<DomContent>();
		final var listCSSEntries = new ArrayList<String>();

		toolkit.makeDocumentBaseStyles(listCSSEntries);

		if (event.error() != null) {
			toolkit.makeDocumentTitleError(env.lang(), event, listBodyContent, false, false);
		}

		final var result = event.result();
		if (result != null) {
			toolkit.makeDocumentTitleWithResult(env.lang(), event, listBodyContent);
		} else if (event.error() == null) {
			toolkit.makeDocumentTitleWithoutResult(env.lang(), event, listBodyContent);
		}

		toolkit.makeDocumentSimpleContext(
				env.lang(), event, listBodyContent, listCSSEntries, env.sendAsSimpleNotificationContextPredicate());

		Optional.ofNullable(event.steps())
				.ifPresent(steps -> toolkit.stepsList(env.lang(), event, false, listBodyContent, listCSSEntries));

		listBodyContent.add(hr());
		toolkit.makeDocumentFooter(listBodyContent, listCSSEntries);

		return new HtmlCssDocumentPayload(listBodyContent, listCSSEntries);
	}

}
