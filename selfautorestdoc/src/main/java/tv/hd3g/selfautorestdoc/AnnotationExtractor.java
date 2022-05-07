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

import static java.util.Arrays.stream;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.commons.authkit.CheckOneBefore;

class AnnotationExtractor {

	private final List<MappingAnnotation> mappings;
	private final List<Set<String>> allCheckBefore;

	AnnotationExtractor(final AnnotatedElement element) {

		mappings = new ArrayList<>();

		if (element.isAnnotationPresent(RequestMapping.class)) {
			final var a = element.getAnnotation(RequestMapping.class);
			mappings.add(new MappingAnnotation(a.name(),
			        a.value(),
			        Arrays.stream(a.method()).collect(Collectors.toUnmodifiableSet()),
			        a.headers(), a.consumes(), a.produces()));
		}
		if (element.isAnnotationPresent(PostMapping.class)) {
			final var a = element.getAnnotation(PostMapping.class);
			mappings.add(new MappingAnnotation(a.name(), a.value(), Set.of(POST),
			        a.headers(), a.consumes(), a.produces()));
		}
		if (element.isAnnotationPresent(PatchMapping.class)) {
			final var a = element.getAnnotation(PatchMapping.class);
			mappings.add(new MappingAnnotation(a.name(), a.value(), Set.of(PATCH),
			        a.headers(), a.consumes(), a.produces()));
		}
		if (element.isAnnotationPresent(PutMapping.class)) {
			final var a = element.getAnnotation(PutMapping.class);
			mappings.add(new MappingAnnotation(a.name(), a.value(), Set.of(PUT),
			        a.headers(), a.consumes(), a.produces()));
		}
		if (element.isAnnotationPresent(DeleteMapping.class)) {
			final var a = element.getAnnotation(DeleteMapping.class);
			mappings.add(new MappingAnnotation(a.name(), a.value(), Set.of(DELETE),
			        a.headers(), a.consumes(), a.produces()));
		}
		if (element.isAnnotationPresent(GetMapping.class)) {
			final var a = element.getAnnotation(GetMapping.class);
			mappings.add(new MappingAnnotation(a.name(), a.value(), Set.of(GET),
			        a.headers(), a.consumes(), a.produces()));
		}

		allCheckBefore = Stream.concat(
		        stream(element.getAnnotationsByType(CheckBefore.class)),
		        stream(element.getAnnotationsByType(CheckOneBefore.class)).flatMap(audits -> stream(audits.value())))
		        .distinct().map(cb -> Arrays.stream(cb.value()).distinct().collect(Collectors.toUnmodifiableSet()))
		        .collect(Collectors.toUnmodifiableList());
	}

	public static class MappingAnnotation {
		private final Optional<String> requestName;
		private final Optional<String> requestValuePath;
		private final Set<RequestMethod> requestMethods;
		private final List<String> headers;
		private final List<String> consumes;
		private final List<String> produces;

		private MappingAnnotation(final String requestName,
		                          final String[] requestValuePaths,
		                          final Set<RequestMethod> requestMethods,
		                          final String[] headers,
		                          final String[] consumes,
		                          final String[] produces) {
			this.requestName = Optional.ofNullable(requestName);
			requestValuePath = Arrays.stream(requestValuePaths).findFirst();
			this.requestMethods = requestMethods;
			this.headers = getOptionalList(headers);
			this.consumes = getOptionalList(consumes);
			this.produces = getOptionalList(produces);
		}

		private List<String> getOptionalList(final String[] value) {
			Objects.requireNonNull(value, "value can't to be null");
			return Arrays.stream(value).collect(Collectors.toUnmodifiableList());
		}

		public Set<RequestMethod> getRequestMethods() {
			return requestMethods;
		}

		public Optional<String> getRequestName() {
			return requestName;
		}

		public Optional<String> getRequestValuePath() {
			return requestValuePath;
		}

		public List<String> getHeaders() {
			return headers;
		}

		public List<String> getConsumes() {
			return consumes;
		}

		public List<String> getProduces() {
			return produces;
		}
	}

	public List<MappingAnnotation> getMappings() {
		return mappings;
	}

	public List<Set<String>> getAllCheckBefore() {
		return allCheckBefore;
	}
}
