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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

class RESTMethod {

	private final Method method;
	private final Optional<CtMethod<?>> methodType;
	private final AnnotationExtractor annotations;
	private final List<URLVariable> urlParameters;
	private final List<URLVariable> urlQueryParameters;
	private final Optional<DtoAnalyser> requestBodyType;
	private final Optional<DtoAnalyser> methodReturn;

	RESTMethod(final Method method, final CtModel model) {
		this.method = Objects.requireNonNull(method, "\"method\" can't to be null");
		annotations = new AnnotationExtractor(method);
		final var paramList = Arrays.asList(method.getParameters());

		urlParameters = paramList.stream()
		        .filter(p -> p.isAnnotationPresent(PathVariable.class))
		        .map(p -> {
			        final var a = p.getAnnotation(PathVariable.class);
			        final var name = a.value();
			        final var type = p.getType();
			        final var required = a.required();
			        return new URLVariable(name, type, required);
		        })
		        .collect(Collectors.toUnmodifiableList());
		urlQueryParameters = paramList.stream()
		        .filter(p -> p.isAnnotationPresent(RequestParam.class))
		        .map(p -> {
			        final var a = p.getAnnotation(RequestParam.class);
			        var name = a.value();
			        if (name == null || name.isEmpty()) {
				        name = p.getName();
			        }
			        final var type = p.getType();
			        final var required = a.required();
			        final var defaultValue = a.defaultValue();
			        if (ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
				        return new URLVariable(name, type, required);
			        }
			        return new URLVariable(name, type, required, defaultValue);
		        })
		        .collect(Collectors.toUnmodifiableList());

		/**
		 * Accept method
		 */
		requestBodyType = paramList.stream()
		        .filter(p -> p.isAnnotationPresent(RequestBody.class))
		        .findFirst().map(Parameter::getType)
		        .flatMap(type -> model.getAllTypes().stream()
		                .filter(t -> type.getName().equals(t.getQualifiedName()))
		                .findFirst())
		        .map(CtType::getReference)
		        .map(r -> new DtoAnalyser(r, false));

		final var controller = model.getAllTypes().stream()
		        .filter(t -> method.getDeclaringClass().getName().equals(t.getQualifiedName()))
		        .findFirst();

		/**
		 * Return method
		 */
		methodType = controller.map(t -> t.getMethodsByName(method.getName()))
		        .flatMap(m -> m.stream().findFirst());

		if (methodType.isEmpty()) {
			methodReturn = Optional.empty();
			return;
		}
		final var mainType = methodType.get().getType();
		final var mainReturnTypeName = mainType
		        .getQualifiedName();
		if (ResponseEntity.class.getName().equals(mainReturnTypeName) == false) {
			/**
			 * Not ResponseEntity<?>
			 */
			methodReturn = Optional.empty();
		} else {
			final var typeEntity = mainType.getActualTypeArguments().stream().findFirst();
			if (typeEntity.isPresent() == false) {
				/**
				 * Not ResponseEntity<RepresentationModel>
				 * Not ResponseEntity<? as RepresentationModel>
				 */
				methodReturn = Optional.empty();
			} else {
				/**
				 * Not ResponseEntity<T as RepresentationModel>
				 * -> T
				 */
				methodReturn = Optional.ofNullable(new DtoAnalyser(typeEntity.get(), true));
			}
		}
	}

	public AnnotationExtractor getAnnotations() {
		return annotations;
	}

	public Optional<DtoAnalyser> getMethodReturn() {
		return methodReturn;
	}

	public Method getMethod() {
		return method;
	}

	public Optional<DtoAnalyser> getRequestBodyType() {
		return requestBodyType;
	}

	public List<URLVariable> getUrlParameters() {
		return urlParameters;
	}

	public List<URLVariable> getUrlQueryParameters() {
		return urlQueryParameters;
	}

	public static class URLVariable {

		private final String name;
		private final Class<?> type;
		private final boolean required;
		private final String defaultValue;

		private URLVariable(final String name, final Class<?> type, final boolean required) {
			this.name = name;
			this.type = type;
			this.required = required;
			defaultValue = null;
		}

		private URLVariable(final String name, final Class<?> type, final boolean required, final String defaultValue) {
			this.name = name;
			this.type = type;
			this.required = required;
			this.defaultValue = defaultValue;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getName() {
			return name;
		}

		public Class<?> getType() {
			return type;
		}

		public boolean isRequired() {
			return required;
		}
	}

	public Optional<File> getControllerFile() {
		return methodType.map(CtElement::getPosition).flatMap(p -> {
			if (p.isValidPosition()) {
				return Optional.ofNullable(p.getFile());
			}
			return Optional.empty();
		});
	}

	public Optional<Integer> getMethodPosInControllerFile() {
		return methodType.map(CtElement::getPosition).flatMap(p -> {
			if (p.isValidPosition()) {
				return Optional.ofNullable(p.getLine());
			}
			return Optional.empty();
		});
	}

	public List<String> getMethodComments() {
		return methodType.map(CtElement::getComments).stream()
		        .flatMap(Collection::stream)
		        .filter(c -> CommentType.JAVADOC.equals(c.getCommentType()))
		        .map(CtComment::getContent)
		        .collect(Collectors.toUnmodifiableList());
	}

}
