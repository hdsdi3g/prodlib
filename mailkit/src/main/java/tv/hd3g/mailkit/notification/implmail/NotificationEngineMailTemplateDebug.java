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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import j2html.tags.DomContent;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;

public class NotificationEngineMailTemplateDebug implements NotificationMailMessageProducer {

	private final NotificationMailTemplateToolkit toolkit;

	public NotificationEngineMailTemplateDebug(final NotificationMailTemplateToolkit toolkit) {
		this.toolkit = Objects.requireNonNull(toolkit, "\"toolkit\" can't to be null");
	}

	@Override
	public NotificationMailMessage makeMessage(final NotificationMailMessageProducerEnvironment env,
											   final SupervisableEndEvent event) {
		return new NotificationMailMessage(
				toolkit.processSubject(env.lang(), event),
				toolkit.processHTMLMessage(assembleHTMLMessageBodyContent(env.lang(), event)));
	}

	private HtmlCssDocumentPayload assembleHTMLMessageBodyContent(final Locale lang,
																  final SupervisableEndEvent event) {
		final var listBodyContent = new ArrayList<DomContent>();
		final var listCSSEntries = new ArrayList<String>();

		toolkit.makeDocumentBaseStyles(listCSSEntries);

		if (event.error() != null) {
			toolkit.makeDocumentTitleError(lang, event, listBodyContent, true, true);
		}

		final var result = event.result();
		if (result != null) {
			toolkit.makeDocumentTitleWithResult(lang, event, listBodyContent);
		} else if (event.error() == null) {
			toolkit.makeDocumentTitleWithoutResult(lang, event, listBodyContent);
		}

		toolkit.makeDocumentDates(lang, event, listBodyContent);
		toolkit.makeDocumentContext(lang, event, listBodyContent, listCSSEntries);

		Optional.ofNullable(event.steps())
				.ifPresent(steps -> toolkit.stepsList(lang, event, true, listBodyContent, listCSSEntries));

		if (result != null) {
			toolkit.makeDocumentCallers(lang, event, listBodyContent, listCSSEntries);
		}

		listBodyContent.add(hr());
		toolkit.makeDocumentEventEnv(lang, event, listBodyContent, listCSSEntries);
		toolkit.makeDocumentFooter(listBodyContent, listCSSEntries);

		return new HtmlCssDocumentPayload(listBodyContent, listCSSEntries);
	}

}
