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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import tv.hd3g.selfautorestdoc.AnnotationExtractor.MappingAnnotation;
import tv.hd3g.selfautorestdoc.DtoAnalyser.DtoItem;
import tv.hd3g.selfautorestdoc.RESTController.RESTControllerAnalysis;
import tv.hd3g.selfautorestdoc.RESTMethod.URLVariable;

class RESTEntryPoint {

	private final RESTControllerAnalysis controller;
	private final RESTMethod method;

	RESTEntryPoint(final RESTControllerAnalysis controller, final RESTMethod method) {
		this.controller = controller;
		this.method = method;
	}

	/**
	 * @return POST, GET, HEAD
	 */
	public String getVerbs() {
		return Stream.concat(
				controller.getAnnotations().getMappings().stream()
						.flatMap(mapping -> mapping.getRequestMethods().stream()),
				method.getAnnotations().getMappings().stream()
						.flatMap(mapping -> mapping.getRequestMethods().stream()))
				.map(Enum::name)
				.collect(Collectors.joining(", "));
	}

	/**
	 * @return like (AA & BB | CC & DD) & (EE)
	 */
	public String getRights() {
		final Function<Set<String>, String> andJoin = checkBefore -> checkBefore.stream()
				.collect(Collectors.joining(" & "));

		final var controllerCB = controller.getAnnotations().getAllCheckBefore().stream()
				.map(andJoin)
				.collect(Collectors.joining(" | "));
		final var methodCB = method.getAnnotations().getAllCheckBefore().stream()
				.map(andJoin)
				.collect(Collectors.joining(" | "));

		if (controllerCB.isEmpty() && methodCB.isEmpty()) {
			return "";
		} else if (controllerCB.isEmpty() ^ methodCB.isEmpty()) {
			return controllerCB.trim() + methodCB.trim();
		} else {
			return "(" + controllerCB.trim() + ") & (" + methodCB.trim() + ")";
		}
	}

	/**
	 * @return [a,b,c,/d,e/,/f/] -> /a/b/c/d/e/f
	 */
	private static String ensurePaths(final String... values) {
		return Arrays.stream(values)
				.filter(v -> v != null && v.isBlank() == false)
				.map(String::trim)
				.map(v -> {
					if (v.equals("/")) {
						return "";
					}
					var start = 0;
					if (v.startsWith("/")) {
						start = 1;
					}
					var ends = v.length();
					if (v.endsWith("/")) {
						ends = v.length() - 1;
					}
					return v.substring(start, ends);
				})
				.collect(Collectors.joining("/", "/", ""));
	}

	/**
	 * @return like [/aa/bbb/{ccc}, /aaa/bb/{eee}/ddd, /dsds]
	 */
	private List<String> getAllRequestValuePaths() {
		final Function<MappingAnnotation, String> getPathsFromControllers = controllerAnnotations -> controllerAnnotations
				.getRequestValuePath().orElse("/");

		final Function<String, Stream<String>> flatCtrlPathMethodsPathsMapper = globalPath -> {
			final Function<MappingAnnotation, String> getFullPathMapper = methodAnnotations -> ensurePaths(
					globalPath, methodAnnotations.getRequestValuePath().orElse(""));
			return method.getAnnotations().getMappings().stream().map(getFullPathMapper);
		};

		return controller.getAnnotations().getMappings().stream()
				.map(getPathsFromControllers)
				.flatMap(flatCtrlPathMethodsPathsMapper)
				.toList();
	}

	/**
	 * @return like /aa/bbb/{ccc}, /aaa/bb/{eee}/ddd?id=1&bool=yes, /dsds
	 */
	public String getPaths() {
		final var allRequestValuePathsStream = getAllRequestValuePaths().stream();
		if (method.getUrlQueryParameters().isEmpty()) {
			return allRequestValuePathsStream.collect(Collectors.joining(", "));
		}

		final var queryParameters = method.getUrlQueryParameters().stream()
				.map(qp -> {
					final Supplier<String> getTypeName = () -> qp.getName() + "=<" + qp.getType().getSimpleName() + ">";
					return Optional.ofNullable(qp.getDefaultValue())
							.map(defaultValue -> qp.getName() + "=" + defaultValue)
							.orElseGet(getTypeName);
				})
				.collect(Collectors.joining("&", "?", ""));
		return allRequestValuePathsStream.collect(Collectors.joining(", ", "", queryParameters));
	}

	private Stream<String> flatMapFromAnnotationMappings(final Function<MappingAnnotation, Stream<String>> mapper) {
		return Stream.concat(
				controller.getAnnotations().getMappings().stream().flatMap(mapper),
				method.getAnnotations().getMappings().stream().flatMap(mapper))
				.filter(n -> n.isBlank() == false);
	}

	public String getRequestNames() {
		return flatMapFromAnnotationMappings(a -> a.getRequestName().stream()).collect(Collectors.joining(", "));
	}

	public String getHeaders() {
		return flatMapFromAnnotationMappings(a -> a.getHeaders().stream()).collect(Collectors.joining(", "));
	}

	public String getProduces() {
		return flatMapFromAnnotationMappings(a -> a.getProduces().stream()).collect(Collectors.joining(", "));
	}

	public String getConsumes() {
		return flatMapFromAnnotationMappings(a -> a.getConsumes().stream()).collect(Collectors.joining(", "));
	}

	/**
	 * @return uuid -> String
	 */
	public Map<String, String> getUrlParameters() {
		return method.getUrlParameters().stream()
				.collect(Collectors.toMap(URLVariable::getName, k -> {
					var opt = "";
					if (k.isRequired() == false) {
						opt = " not required";
					}
					return k.getType().getSimpleName() +
						   Optional.ofNullable(k.getDefaultValue()).map(v -> " (" + v + ") ").orElse("") +
						   opt;
				}, (v1, v2) -> v1, LinkedHashMap::new));
	}

	private String dtoFormatter(final DtoItem item) {
		final var sb = new StringBuilder();
		final var prefix = StringUtils.repeat("    ", item.getStratumPos() + 1);
		sb.append(prefix);
		sb.append(item.getName());
		sb.append(": ");

		switch (item.getStructuralStratum()) {
		case LIST:
			sb.append("[");

			if (item.getSubItems() != null) {
				sb.append("{");
				sb.append(System.lineSeparator());
				sb.append(item.getSubItems().stream().map(this::dtoFormatter)
						.collect(Collectors.joining(System.lineSeparator())));
				sb.append(System.lineSeparator());
				sb.append(prefix);
				sb.append("}");
			} else {
				sb.append(item.getType());
			}

			sb.append(", ...]");
			if (item.isLastItem() == false) {
				sb.append(",");
			}
			break;
		case MAP:
			sb.append("{");
			sb.append(System.lineSeparator());
			sb.append(item.getSubItems().stream().map(this::dtoFormatter)
					.collect(Collectors.joining(System.lineSeparator())));
			sb.append(System.lineSeparator());
			sb.append(prefix);
			sb.append("}");
			if (item.isLastItem() == false) {
				sb.append(",");
			}
			break;
		default:
			sb.append(item.getType());
			if (item.isLastItem() == false) {
				sb.append(",");
			}
			break;
		}
		return sb.toString();
	}

	public Optional<List<String>> getDTORequest() {
		final var bodyRequest = method.getRequestBodyType();
		if (bodyRequest.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(bodyRequest.get().getDtoContent().stream()
				.map(this::dtoFormatter)
				.toList());
	}

	public Optional<List<String>> getDTOResponse() {
		final var methodReturn = method.getMethodReturn();
		if (methodReturn.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(methodReturn.get().getDtoContent().stream()
				.map(this::dtoFormatter)
				.toList());
	}

	public String getControllerSimpleName() {
		return controller.getControllerClass().getSimpleName();
	}

	public String getControllerFullPath() {
		final var ctrl = controller.getControllerClass().getName().replace(".", "/");
		final var hash = ctrl.indexOf('#');
		if (hash > 0) {
			return ctrl.substring(0, hash - 1);
		}
		return method.getControllerFile()
				.map(cFile -> {
					final var f = cFile.getAbsolutePath();
					final var relative = new File("").getAbsolutePath();
					return f.substring(relative.length() + 1);
				})
				.map(f -> f.replace('\\', '/'))
				.orElse(ctrl);
	}

	public String getMethodName() {
		return method.getMethod().getName();
	}

	public int getLineMethodInController() {
		return method.getMethodPosInControllerFile().orElse(1);
	}

	public List<String> getMethodComments() {
		return method.getMethodComments();
	}

}
