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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import spoon.reflect.CtModel;

class RESTController {

	private final Class<?> controllerClass;

	RESTController(final Class<?> controllerClass) {
		this.controllerClass = Objects.requireNonNull(controllerClass, "\"controllerClass\" can't to be null");
	}

	private static boolean isAnnotationMapping(Annotation a){return a.annotationType() == RequestMapping.class ||
	                                                                      a.annotationType() == PostMapping.class ||
	                                                                      a.annotationType() == PatchMapping.class ||
	                                                                      a.annotationType() == PutMapping.class ||
	                                                                      a.annotationType() == DeleteMapping.class ||
	                                                                      a.annotationType() == GetMapping.class;
	}

	public RESTControllerAnalysis processAnalysis(final CtModel model) {
		final var classAnnotations = new AnnotationExtractor(controllerClass);
		final var methods = Arrays.stream(controllerClass.getDeclaredMethods())
		        .filter(m -> Modifier.isPublic(m.getModifiers()))
		        .filter(m -> Arrays.stream(m.getAnnotations()).anyMatch(RESTController::isAnnotationMapping))
		        .map(m -> new RESTMethod(m, model)).collect(Collectors.toUnmodifiableList());

		return new RESTControllerAnalysis(classAnnotations, methods);
	}

	public class RESTControllerAnalysis {
		private final AnnotationExtractor classAnnotations;
		private final List<RESTMethod> methods;

		private RESTControllerAnalysis(final AnnotationExtractor classAnnotations,
		                               final List<RESTMethod> methods) {
			this.classAnnotations = classAnnotations;
			this.methods = methods;
		}

		public AnnotationExtractor getAnnotations() {
			return classAnnotations;
		}

		public List<RESTMethod> getMethods() {
			return methods;
		}

		public Class<?> getControllerClass() {
			return controllerClass;
		}
	}
}
