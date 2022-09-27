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
import static j2html.TagCreator.pre;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.style;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static j2html.attributes.Attr.HTTP_EQUIV;
import static java.util.function.Predicate.not;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.utils.EscapeUtil;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.jobkit.engine.SupervisableMessage;
import tv.hd3g.jobkit.engine.SupervisableResult;
import tv.hd3g.jobkit.engine.SupervisableResultState;
import tv.hd3g.jobkit.engine.SupervisableStep;
import tv.hd3g.mailkit.mod.component.Translate;
import tv.hd3g.mailkit.notification.ExceptionToString;
import tv.hd3g.mailkit.notification.NotificationEnvironment;

public class NotificationMailTemplateToolkit {
	private static Logger log = LogManager.getLogger();

	static final String APPNAME = ".appname";

	protected final Translate translate;
	protected final NotificationEnvironment env;
	private final ExceptionToString exceptionToString;

	public NotificationMailTemplateToolkit(final Translate translate, final NotificationEnvironment env) {
		this.translate = Objects.requireNonNull(translate, "\"translate\" can't to be null");
		this.env = Objects.requireNonNull(env, "\"env\" can't to be null");
		exceptionToString = new ExceptionToString();
	}

	public String processSubject(final Locale lang, final SupervisableEndEvent event) {
		if (event.error() != null) {
			return translate.i18n(lang, event, "subject.error",
					"Error for {0}: {1}", env.appName(), event.error().getMessage());
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
	 * @return span tag to raw string, with attrs as class and escape(textToEscape) in it.
	 */
	String escapeNspanWrapp(final String attrs, final String textToEscape) {
		return span(attrs(attrs), EscapeUtil.escape(textToEscape)).render();
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

		return li(timeSpan, messageSpan, verboseSpan);
	}

	public void stepsList(final Locale lang,
						  final SupervisableEndEvent event,
						  final boolean verbose,
						  final List<DomContent> listBodyContent,
						  final List<String> listCSSEntries) {

		final var stepLines = each(event.steps(),
				step -> stepLineToString(lang, event, step, verbose));
		listBodyContent.add(ul(attrs(".steps"), stepLines));
		listCSSEntries.add("""
				ul.steps {
				    display: block;
				    font-family: monospace;
				    margin: 1em;
				    padding: 0.9em;
				    border-radius: 0.8rem;
				    background-color: #F8F8F8;
				    color: #000;
				    border: 1px solid #cfcfcf;
				    list-style-type: none;
				}
				ul.steps span.stepdate {
				   color: #CCC;
				}
				ul.steps span.verbose {
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
				    font-family: monospace;
				    color: #000;
				}

				span.date {
				    font-family:  sans-serif;
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
				}
				""");
	}

	public void makeDocumentTitleWithoutResult(final Locale lang,
											   final SupervisableEndEvent event,
											   final List<DomContent> listBodyContent) {
		final var title = translate.i18n(lang, event, "title.ok", "Process done for {0}",
				escapeNspanWrapp(APPNAME, env.appName()));
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
				escapeNspanWrapp(".resultstate." + cssResultState, getResultStateI18n(lang, event)),
				escapeNspanWrapp(APPNAME, env.appName()));
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
				escapeNspanWrapp(APPNAME, env.appName()));
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
			}
			final var subTitle = translate.i18n(lang, event, "title.error.message", "{0}",
					escapeNspanWrapp(".errormessage", message));
			listBodyContent.add(h3(rawHtml(subTitle)));
		}
	}

	public void makeDocumentDates(final Locale lang,
								  final SupervisableEndEvent event,
								  final List<DomContent> listBodyContent) {
		final var dates = translate.i18n(lang, event, "dates",
				"Created the {0}, started at {1}, ended at {2}.",
				escapeNspanWrapp(".event.date.create", formatLongDate(event.creationDate(), lang)),
				escapeNspanWrapp(".event.date.start", formatShortTime(event.startDate(), lang)),
				escapeNspanWrapp(".event.date.end", formatShortTime(event.endDate(), lang)));
		listBodyContent.add(div(attrs(".dates"), rawHtml(dates)));
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
				""");
		final var context = event.context();
		if (context != null) {
			listBodyContent.add(div(attrs(".contextblock"),
					text(translate.i18n(lang, event, "context", "Context:")),
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
				""");

		final var resultCaller = translate.i18n(lang, event, "result.caller", "Result caller source: {0}.",
				escapeNspanWrapp(".result.caller", callerToString(event.result().caller())));
		final var resultDate = translate.i18n(lang, event, "result.date", "Result caller provided at {0}.",
				escapeNspanWrapp(".result.date", formatShortDate(event.result().date(), lang)));
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
				span.envevent {
				    font-family: monospace;
				    color: #AAA;
				}
				""");
		final var senderSource = translate.i18n(lang, event, "sender",
				"Notification type: {0}, provided by {1}, runned on spool {2} as {3}, sended by {4} for {5}.",
				escapeNspanWrapp(".envevent.type", event.typeName()),
				escapeNspanWrapp(".envevent.manager", event.managerName()),
				escapeNspanWrapp(".envevent.spool", event.spoolName()),
				escapeNspanWrapp(".envevent.job", event.jobName()),
				escapeNspanWrapp(".envevent.instance", env.instanceName()),
				escapeNspanWrapp(".envevent.vendor", env.vendorName()));
		listBodyContent.add(div(attrs(".sendersource"), rawHtml(senderSource)));
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
