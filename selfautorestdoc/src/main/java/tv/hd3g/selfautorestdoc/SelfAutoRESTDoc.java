/*
 * This file is part of SelfAutoRESTDoc.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.selfautorestdoc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.springframework.web.bind.annotation.RestController;

import spoon.Launcher;

public class SelfAutoRESTDoc {

	private final ConcurrentHashMap<Class<?>, RESTController> restControllerAutoDocByClass;
	public static final String NEW_LINE = "\n";

	public SelfAutoRESTDoc() {
		restControllerAutoDocByClass = new ConcurrentHashMap<>();
	}

	public void registerClass(final Class<?> cl) {
		if (cl.isAnnotationPresent(RestController.class)) {
			restControllerAutoDocByClass.computeIfAbsent(cl, RESTController::new);
		}
	}

	public void writeToMD(final File mdFile, final String baseProjectURL) throws IOException {
		final var entryPoints = processAnalysis();

		try (final var pw = new PrintWriter(mdFile, UTF_8)) {
			pw.print("# REST API" + NEW_LINE);
			pw.print(NEW_LINE);
			writeSummary(entryPoints, pw);
			entryPoints.forEach(ep -> writeContent(ep, pw, baseProjectURL));
			pw.print(NEW_LINE);
		}
	}

	private List<RESTEntryPoint> processAnalysis() {
		final var launcher = new Launcher();
		launcher.addInputResource("src/main/java");
		launcher.buildModel();
		final var model = launcher.getModel();

		return restControllerAutoDocByClass.values().stream()
		        .map(c -> c.processAnalysis(model))
		        .flatMap(controller -> controller.getMethods().stream()
		                .map(method -> new RESTEntryPoint(controller, method)))
		        .sorted((l, r) -> (l.getPaths() + l.getVerbs()).compareTo(r.getPaths() + r.getVerbs()))
		        .collect(Collectors.toUnmodifiableList());
	}

	private static String linkifyTitle(final String title) {
		final var c = new CRC32();
		c.update(title.getBytes(UTF_8));
		return "goto" + Math.abs(c.getValue());
	}

	private void writeSummary(final List<RESTEntryPoint> entryPoints, final PrintWriter pw) {
		pw.print("## Summary <a name=\"top\"></a>" + NEW_LINE);
		pw.print(NEW_LINE);

		entryPoints.forEach(ep -> {
			pw.print("* [");
			final var link = ep.getRequestNames() + " **" + ep.getVerbs() + "** `" + ep.getPaths() + "`";
			pw.print(link.trim());
			pw.print("](#");
			pw.print(linkifyTitle(ep.getRequestNames() + ep.getVerbs() + ep.getPaths()));
			pw.print(")");
			pw.print(NEW_LINE);
		});
	}

	private void writeContent(final RESTEntryPoint ep, final PrintWriter pw, final String baseProjectURL) {
		pw.print(NEW_LINE);
		pw.print("## ");
		final var rqname = ep.getRequestNames();

		pw.print("<a name=\"");
		pw.print(linkifyTitle(rqname + ep.getVerbs() + ep.getPaths()));
		pw.print("\"></a> ");

		if (rqname.isEmpty() == false) {
			pw.print(rqname + NEW_LINE);
		}
		pw.print("**" + ep.getVerbs() + "** ");
		pw.print("`" + ep.getPaths() + "`" + NEW_LINE);
		pw.print(NEW_LINE);

		ep.getMethodComments().forEach(c -> {
			pw.print(c + NEW_LINE);
			pw.print(NEW_LINE);
		});
		if (ep.getMethodComments().isEmpty()) {
			pw.print(ep.getMethodName() + NEW_LINE);
			pw.print(NEW_LINE);
		}

		final var urlParameters = ep.getUrlParameters();
		if (urlParameters.isEmpty() == false) {
			pw.print("Parameters:" + NEW_LINE);
			urlParameters.forEach((n, t) -> pw.print(" - **" + n + "** " + t + NEW_LINE));
			pw.print(NEW_LINE);
		}
		final var headers = ep.getHeaders();
		if (rqname.isEmpty() == false) {
			pw.print("Headers: ");
			pw.print(headers + NEW_LINE);
			pw.print(NEW_LINE);
		}

		ep.getDTORequest().ifPresent(r -> {
			pw.print("```javascript" + NEW_LINE);
			pw.print("Request body data: ");
			final var consumes = ep.getConsumes();
			if (consumes.isBlank() == false) {
				pw.print("\"");
				pw.print(consumes);
				pw.print("\" ");
			}
			pw.print("{" + NEW_LINE);
			r.forEach(l -> pw.print(l + NEW_LINE));
			pw.print("}" + NEW_LINE);
			pw.print("```" + NEW_LINE);
			pw.print(NEW_LINE);
		});

		ep.getDTOResponse().ifPresent(r -> {
			pw.print("```javascript" + NEW_LINE);
			pw.print("Response: ");
			final var produces = ep.getProduces();
			if (produces.isBlank() == false) {
				pw.print("\"");
				pw.print(produces);
				pw.print("\" ");
			}
			pw.print("{" + NEW_LINE);
			r.forEach(l -> pw.print(l + NEW_LINE));
			pw.print("}" + NEW_LINE);
			pw.print("```" + NEW_LINE);
			pw.print(NEW_LINE);
		});

		final var rights = ep.getRights();
		if (rights.isEmpty() == false) {
			pw.print("_Mandatory rights: ");
			pw.print(rights + "_" + NEW_LINE);
			pw.print(NEW_LINE);
		}

		pw.print("[Go to the top](#top)");
		pw.print(" &bull; [" + ep.getControllerSimpleName());
		pw.print(" :: ");
		pw.print(ep.getMethodName());

		pw.print("](" + baseProjectURL + "/");
		pw.print(ep.getControllerFullPath());
		pw.print("#");
		pw.print(ep.getLineMethodInController());
		pw.print(")");
		pw.print(NEW_LINE);
	}

}
