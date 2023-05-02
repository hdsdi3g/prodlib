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

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.li;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.style;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static j2html.attributes.Attr.HTTP_EQUIV;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.function.Predicate.not;
import static tv.hd3g.mailkit.notification.ExceptionToString.exceptionRefCleaner;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import j2html.TagCreator;
import j2html.tags.DomContent;
import tv.hd3g.commons.version.EnvironmentVersion;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.jobkit.engine.SupervisableMessage;
import tv.hd3g.jobkit.engine.SupervisableResult;
import tv.hd3g.jobkit.engine.SupervisableResultState;
import tv.hd3g.jobkit.engine.SupervisableStep;
import tv.hd3g.mailkit.mod.component.Translate;
import tv.hd3g.mailkit.mod.service.SendAsSimpleNotificationContextPredicate;
import tv.hd3g.mailkit.notification.ExceptionToString;
import tv.hd3g.mailkit.notification.NotificationEnvironment;

public class NotificationMailTemplateToolkit {
	private static final String ATTR_JSON = ".json";
	private static final String ATTR_JSON_VALUE = ".json.value";
	private static final String CONTEXT_STR = "Context:";

	private static Logger log = LogManager.getLogger();

	static final String APPNAME = ".appname";

	protected final Translate translate;
	protected final NotificationEnvironment env;
	private final ExceptionToString exceptionToString;
	private final EnvironmentVersion environmentVersion;

	public NotificationMailTemplateToolkit(final Translate translate,
										   final NotificationEnvironment env,
										   final EnvironmentVersion environmentVersion) {
		this.translate = Objects.requireNonNull(translate, "\"translate\" can't to be null");
		this.env = Objects.requireNonNull(env, "\"env\" can't to be null");
		this.environmentVersion = Objects.requireNonNull(environmentVersion, "\"environmentVersion\" can't to be null");
		exceptionToString = new ExceptionToString();
	}

	public String processSubject(final Locale lang, final SupervisableEndEvent event) {
		if (event.error() != null) {
			return translate.i18n(lang, event, "subject.error",
					"Error for {0}: {1}", env.appName(), exceptionRefCleaner(event.error().getMessage()));
		}
		final var result = event.result();
		if (result != null) {
			final var resultState = getResultStateI18n(lang, event);

			final var message = Optional.ofNullable(event.result().message())
					.map(m -> translateMessage(lang, event, m))
					.orElse(null);
			if (message != null) {
				return translate.i18n(lang, event, "subject.okwithresult",
						"Process {0} for {1}: {2}",
						resultState, env.appName(), message);
			} else {
				return translate.i18n(lang, event, "subject.okwithresultmessageless",
						"Process {0} for {1}",
						resultState, env.appName());
			}
		}
		final var subject = translate.i18n(lang, event, "subject.ok",
				"Process done for {0}", env.appName());
		log.trace("Subject: \"{}\"", subject);
		return subject;
	}

	public String processHTMLMessage(final HtmlCssDocumentPayload payload) {
		final var style = payload.listCSSEntries().stream()
				.map(e -> e.split("\\r?\\n"))
				.map(List::of)
				.flatMap(List::stream)
				.map(String::trim)
				.filter(not(String::isEmpty))
				.collect(Collectors.joining("\r\n"));

		return String.join("\r\n",
				"<!DOCTYPE html>",
				html(
						head(
								meta()
										.withName("viewport")
										.withContent("width=device-width, initial-scale=1.0"),
								meta()
										.withData(HTTP_EQUIV, "Content-Type")
										.withContent("text/html; charset=UTF-8"),
								style(style)
										.withType("text/css")),
						body(each(payload.listBodyContent().stream())))
								.withLang("en")
								.renderFormatted())
				.trim();
	}

	public String getResultStateI18n(final Locale lang, final SupervisableEndEvent event) {
		return Optional.ofNullable(event.result())
				.map(SupervisableResult::state)
				.flatMap(Optional::ofNullable)
				.map(SupervisableResultState::name)
				.map(String::toLowerCase)
				.map(stateName -> translate.i18n(lang, event, "resultstate." + stateName, stateName))
				.orElse("done");
	}

	/**
	 * @return span tag to raw string, with attrs as class in it.
	 */
	String spanWrapp(final String attrs, final String text) {
		return span(attrs(attrs), text).render();
	}

	public String formatLongDate(final Date date, final Locale locale) {
		return Optional.ofNullable(date)
				.map(d -> DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, locale)
						.format(d))
				.orElse("(?)");
	}

	public String formatShortDate(final Date date, final Locale locale) {
		return Optional.ofNullable(date)
				.map(d -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale)
						.format(d))
				.orElse("(?)");
	}

	public String formatShortTime(final Date date, final Locale locale) {
		return Optional.ofNullable(date)
				.map(d -> DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
						.format(d))
				.orElse("(?)");
	}

	DomContent exceptionFormatterToDom(final Exception e, final boolean verbose) {
		final String stackTrace;
		if (verbose) {
			stackTrace = exceptionToString.getStackTrace(e);
		} else {
			stackTrace = exceptionToString.getSimpleStackTrace(e);
		}
		return div(attrs(".stacktrace"), pre(stackTrace));
	}

	public String callerToString(final StackTraceElement caller) {
		final var fileName = caller.getFileName();
		final var lineNumber = caller.getLineNumber();

		if (fileName != null && lineNumber >= 0) {
			return fileName + " L" + lineNumber;
		} else if (fileName != null) {
			return fileName;
		} else {
			return caller.getClassName();
		}
	}

	DomContent stepLineToString(final Locale lang,
								final SupervisableEndEvent event,
								final SupervisableStep step,
								final boolean verbose) {
		final var timeSpan = span(attrs(".stepdate"), formatShortTime(step.stepDate(), lang));
		final var messageSpan = span(translateMessage(lang, event, step.message()));

		DomContent verboseSpan = null;
		if (verbose) {
			final var fName = step.caller().getFileName();
			final var line = step.caller().getLineNumber();
			if (line > 0) {
				verboseSpan = span(attrs(".verbose"), fName.substring(0, fName.lastIndexOf(".")) + " L" + line);
			} else {
				verboseSpan = span(attrs(".verbose"), fName.substring(0, fName.lastIndexOf(".")));
			}
		}

		return div(attrs(".stepline"), timeSpan, messageSpan, verboseSpan);
	}

	public void stepsList(final Locale lang,
						  final SupervisableEndEvent event,
						  final boolean verbose,
						  final List<DomContent> listBodyContent,
						  final List<String> listCSSEntries) {
		if (event.steps().isEmpty()) {
			return;
		}
		final var stepLines = each(event.steps(),
				step -> stepLineToString(lang, event, step, verbose));
		listBodyContent.add(div(attrs(".steps"), stepLines));
		listCSSEntries.add("""
				div.steps {
				    display: block;
				    font-family: 'Consolas', 'Monaco', monospace;
				    margin: 1em;
				    padding: 0.9em;
				    border-radius: 0.8rem;
				    background-color: #F8F8F8;
				    color: #000;
				    border: 1px solid #cfcfcf;
				}
				div.steps span.stepdate {
				   color: #CCC;
				}
				div.steps span.verbose {
				   color: #C8C;
				}
				""");
	}

	public void makeDocumentBaseStyles(final List<String> listCSSEntries) {
		listCSSEntries.add("""
				body {
				    font-family: "Tahoma", sans-serif;
				    font-size: 12pt;
				    color: #090909;
				}

				span.caller {
				    font-family: 'Consolas', 'Monaco', monospace;
				    color: #000;
				}

				span.date {
				    font-family: sans-serif;
				    color: #fff;
				    background-color: #85c0ad;
				    display: inline-block;
				    padding: 0.25em 0.4em 0.25em 0.4em;
				    font-size: 90%;
				    font-weight: 700;
				    line-height: 0.8;
				    white-space: nowrap;
				    border-radius: 0.2rem;
				}

				div.stacktrace {
				    margin: 1em;
				    padding: 1em;
				    font-size: 100%;
				    border-radius: 0.8rem;
				    border-width: 5px;
				    border-color: #f3f8f6;
				    border-style: dashed;
				}

				.appname {
				    color: #6d3824;
				}

				h3 {
				    margin-left: 1em;
				    margin-right: 1em;
				    padding: 1em;
				    background-color: #f3f8f6;
				    font-weight: normal;
				    border-radius: 0.8rem;
				}
				pre {
				    margin: 0px;
				    line-height: 1.3;
				    font-family: 'Consolas', 'Monaco', monospace;
				}
				""");
	}

	public void makeDocumentTitleWithoutResult(final Locale lang,
											   final SupervisableEndEvent event,
											   final List<DomContent> listBodyContent) {
		final var title = translate.i18n(lang, event, "title.ok", "Process done for {0}",
				spanWrapp(APPNAME, env.appName()));
		listBodyContent.add(h1(rawHtml(title)));
	}

	public String translateMessage(final Locale lang,
								   final SupervisableEndEvent event,
								   final SupervisableMessage message) {
		return translate.i18n(lang, event, message.code(), message.defaultResult(), message.getVarsArray());
	}

	public void makeDocumentTitleWithResult(final Locale lang,
											final SupervisableEndEvent event,
											final List<DomContent> listBodyContent) {
		final var result = event.result();
		final var cssResultState = result.state().name().toLowerCase();
		final var title = translate.i18n(lang, event, "title.okwithresult",
				"Process {0} for {1}",
				spanWrapp(".resultstate." + cssResultState, getResultStateI18n(lang, event)),
				spanWrapp(APPNAME, env.appName()));
		listBodyContent.add(h1(rawHtml(title)));

		Optional.ofNullable(event.result().message())
				.ifPresent(m -> listBodyContent.add(h3(translateMessage(lang, event, m))));
	}

	public void makeDocumentTitleError(final Locale lang,
									   final SupervisableEndEvent event,
									   final List<DomContent> listBodyContent,
									   final boolean displayVerboseError,
									   final boolean displayFullStackTrace) {
		final var title = translate.i18n(lang, event, "title.error", "Error for {0}",
				spanWrapp(APPNAME, env.appName()));
		listBodyContent.add(h1(rawHtml(title)));

		if (displayVerboseError) {
			if (displayFullStackTrace) {
				listBodyContent.add(exceptionFormatterToDom(event.error(), true));
			} else {
				listBodyContent.add(exceptionFormatterToDom(event.error(), false));
			}
		} else {
			var message = event.error().getMessage();
			if (message == null || message.isEmpty()) {
				message = "\"" + event.error().getClass().getSimpleName() + "\"";
			} else {
				message = exceptionRefCleaner(message);
			}
			final var subTitle = translate.i18n(lang, event, "title.error.message", "{0}",
					spanWrapp(".errormessage", message));
			listBodyContent.add(h3(rawHtml(subTitle)));
		}
	}

	public void makeDocumentDates(final Locale lang,
								  final SupervisableEndEvent event,
								  final List<DomContent> listBodyContent) {
		final var dates = translate.i18n(lang, event, "dates",
				"Created the {0}, started at {1}, ended at {2}.",
				spanWrapp(".event.date.create", formatLongDate(event.creationDate(), lang)),
				spanWrapp(".event.date.start", formatShortTime(event.startDate(), lang)),
				spanWrapp(".event.date.end", formatShortTime(event.endDate(), lang)));
		listBodyContent.add(div(attrs(".dates"), rawHtml(dates)));
	}

	private DomContent jsonToDom(final JsonNode json,
								 final UnaryOperator<String> i18nKey, // NOSONAR 4276
								 final Predicate<String> filterKey) {
		return switch (json.getNodeType()) {
		case ARRAY -> ol(attrs(ATTR_JSON),
				each(IntStream.range(0, json.size())
						.mapToObj(json::get)
						.map(entry -> jsonToDom(entry, i18nKey, filterKey))
						.map(TagCreator::li)
						.map(entry -> (DomContent) entry)));// NOSONAR S1612
		case OBJECT -> ul(attrs(ATTR_JSON),
				each(StreamSupport.stream(
						spliteratorUnknownSize(json.fieldNames(), Spliterator.DISTINCT), false)
						.filter(filterKey)
						.sorted()
						.map(key -> {
							final var value = json.get(key);
							final var keyName = i18nKey.apply(key) + ":";
							return li(span(attrs(".json.key"),
									keyName),
									jsonToDom(value, i18nKey, filterKey));
						})
						.map(entry -> (DomContent) entry)));// NOSONAR S1612

		case NUMBER -> span(attrs(ATTR_JSON_VALUE), json.toString());
		case STRING -> span(attrs(ATTR_JSON_VALUE), json.asText());
		case BOOLEAN -> span(attrs(ATTR_JSON_VALUE), i18nKey.apply(String.valueOf(json.asBoolean())));
		default -> div(ATTR_JSON);
		};
	}

	public void makeDocumentSimpleContext(final Locale lang,
										  final SupervisableEndEvent event,
										  final List<DomContent> listBodyContent,
										  final List<String> listCSSEntries,
										  final SendAsSimpleNotificationContextPredicate contextPredicate) {
		final var context = event.context();
		if (context == null || context.isNull() || context.isEmpty() && context.isContainerNode()) {
			return;
		}

		final var tree = jsonToDom(context,
				k -> translate.i18n(lang, event, "simplecontext.entry." + k, k),
				k -> contextPredicate.isSendAsSimpleNotificationThisContextEntry(k, event));

		listCSSEntries.add("""
				div.simplecontext {
				    margin-top: 1em;
				    margin-bottom: 1em;
				}
				div.simplecontext span.json.value {
				    font-family: 'Consolas', 'Monaco', monospace;
				    color: #555;
				}
				div.simplecontext ul {
				    margin-block-start: 0em;
				    margin-block-end: 0em;
				    list-style-type: none;
				}
				div.simplecontext ol {
				    margin-block-start: 0em;
				    margin-block-end: 0em;
				}
				""");

		listBodyContent.add(
				div(attrs(".simplecontext"),
						text(translate.i18n(lang, event, "context", CONTEXT_STR)),
						tree));
	}

	public void makeDocumentContext(final Locale lang,
									final SupervisableEndEvent event,
									final List<DomContent> listBodyContent,
									final List<String> listCSSEntries) {
		listCSSEntries.add("""
				div.contextblock {
				    margin-top: 1em;
				}

				div.contextblock div.context {
				    margin: 1em;
				    padding: 0.9em;
				    border-radius: 0.8rem;
				    background-color: #484848;
				    color: #dbdbdb;
				}
				div.contextblock div.context a {
				   color: #dbdbdb;
				}
				div.contextblock div.context a:active {
				   color: #dbdbdb;
				}
				div.contextblock div.context a:hover {
				   color: #dbdbdb;
				}
				div.contextblock div.context a:visited {
				   color: #dbdbdb;
				}
				div.contextblock div.context a:link {
				   color: #dbdbdb;
				}
				""");
		final var context = event.context();
		if (context != null) {
			listBodyContent.add(div(attrs(".contextblock"),
					text(translate.i18n(lang, event, "context", CONTEXT_STR)),
					div(attrs(".context"), pre(event.context().toPrettyString()))));
		}
	}

	public void makeDocumentCallers(final Locale lang,
									final SupervisableEndEvent event,
									final List<DomContent> listBodyContent,
									final List<String> listCSSEntries) {
		listCSSEntries.add("""
				div.result.date {
				    display: inline-block;
				}
				div.result.caller {
				    display: inline-block;
				}
				span.result.caller {
				    color: #C8C;
				    font-family: 'Consolas', 'Monaco', monospace;
				}
				""");

		final var resultCaller = translate.i18n(lang, event, "result.caller", "Result caller source: {0}.",
				spanWrapp(".result.caller", callerToString(event.result().caller())));
		final var resultDate = translate.i18n(lang, event, "result.date", "Result caller provided at {0}.",
				spanWrapp(".result.date", formatShortDate(event.result().date(), lang)));
		listBodyContent.add(div(attrs(".result.caller"), rawHtml(resultCaller)));
		listBodyContent.add(div(attrs(".result.date"), rawHtml(resultDate)));
	}

	public void makeDocumentEventEnv(final Locale lang,
									 final SupervisableEndEvent event,
									 final List<DomContent> listBodyContent,
									 final List<String> listCSSEntries) {
		listCSSEntries.add("""
				div.sendersource {
				    color: #BBB
				}
				div.senderversion {
				    color: #BBB
				}
				span.envevent {
				    font-family: 'Consolas', 'Monaco', monospace;
				    color: #AAA;
				}
				""");
		final var senderSource = translate.i18n(lang, event, "sender",
				"Notification type: {0}, provided by {1}, runned on spool {2} as {3}, sended by {4}#{5} (started the {6}) for {7}.",
				spanWrapp(".envevent.type", event.typeName()),
				spanWrapp(".envevent.manager", event.managerName()),
				spanWrapp(".envevent.spool", event.spoolName()),
				spanWrapp(".envevent.job", event.jobName()),
				spanWrapp(".envevent.instance", env.instanceName()),
				spanWrapp(".envevent.instancepid", String.valueOf(environmentVersion.pid())),
				spanWrapp(".envevent.startedon", formatLongDate(environmentVersion.startupTime(), lang)),
				spanWrapp(".envevent.vendor", env.vendorName()));
		listBodyContent.add(div(attrs(".sendersource"), rawHtml(senderSource)));

		final var senderVersion = translate.i18n(lang, event, "senderversion",
				"App version: {0}, deps versions: {1}/{2} runned on {3} {4}.",
				spanWrapp(".envevent.appversion", environmentVersion.appVersion()),
				spanWrapp(".envevent.prodlib", environmentVersion.prodlibVersion()),
				spanWrapp(".envevent.framework", environmentVersion.frameworkVersion()),
				spanWrapp(".envevent.jvmname", environmentVersion.jvmNameVendor()),
				spanWrapp(".envevent.jvmversion", environmentVersion.jvmVersion()));
		listBodyContent.add(div(attrs(".senderversion"), rawHtml(senderVersion)));
	}

	public void makeDocumentFooter(final List<DomContent> listBodyContent, final List<String> listCSSEntries) {
		listCSSEntries.add("""
				div.env.footer {
				    margin-top: 1em;
				}
				""");
		final var envFooter = Optional.ofNullable(env.footer()).stream()
				.map(List::stream)
				.flatMap(l -> l)
				.map(TagCreator::rawHtml)
				.map(r -> div(attrs(".conf"), r))
				.map(DomContent.class::cast);
		listBodyContent.add(div(attrs(".env.footer"), each(envFooter)));
	}
}
