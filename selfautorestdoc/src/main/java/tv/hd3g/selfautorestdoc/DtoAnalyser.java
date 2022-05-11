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

import static tv.hd3g.selfautorestdoc.DtoAnalyser.StructuralStratum.LIST;
import static tv.hd3g.selfautorestdoc.DtoAnalyser.StructuralStratum.MAP;
import static tv.hd3g.selfautorestdoc.DtoAnalyser.StructuralStratum.VALUE;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

class DtoAnalyser {
	private static Logger log = LogManager.getLogger();

	private static final TypeFactory typeFactory = new TypeFactory();

	private static final CtTypeReference<?> charSequenceType = typeFactory.get(CharSequence.class).getReference();
	private static final CtTypeReference<?> collectionType = typeFactory.get(Collection.class).getReference();
	private static final CtTypeReference<?> mapType = typeFactory.get(Map.class).getReference();
	private static final CtTypeReference<?> objectType = typeFactory.get(Object.class).getReference();
	private static final CtTypeReference<?> representationModelType = typeFactory.get(RepresentationModel.class)
	        .getReference();

	private final CtTypeReference<?> declaringType;
	private final boolean isResponseDto;

	DtoAnalyser(final CtTypeReference<?> declaringType, final boolean isResponseDto) {
		this.declaringType = Objects.requireNonNull(declaringType, "\"declaringType\" can't to be null");
		this.isResponseDto = isResponseDto;
	}

	public enum StructuralStratum {
		VALUE,
		LIST,
		MAP;
	}

	public class DtoItem {
		private final int stratumPos;
		private final StructuralStratum structuralStratum;
		private final String name;
		private final CtTypeReference<?> type;
		private final List<DtoItem> subItems;
		private boolean lastItem;

		private DtoItem(final CtTypeReference<?> declaringType,
		                final String name,
		                final CtTypeReference<?> internalType,
		                final int stratumPos) {
			this.stratumPos = stratumPos;
			lastItem = false;
			this.name = name;

			final CtTypeReference<?> originalType;

			if (internalType.isSubtypeOf(collectionType)) {
				structuralStratum = LIST;
				originalType = extractFirstTypeArguments(internalType).orElse(objectType);
			} else if (internalType.isSubtypeOf(mapType)) {
				structuralStratum = MAP;
				originalType = extractFirstTypeArguments(internalType).orElse(objectType);
			} else if (internalType.isPrimitive()
			           || internalType.isSubtypeOf(charSequenceType)
			           || internalType.getPackage() == null
			           || isTypeIsProtected(internalType)) {
				structuralStratum = VALUE;
				originalType = internalType;
			} else {
				structuralStratum = MAP;
				originalType = internalType;
			}

			if (originalType.isGenerics() && declaringType.getActualTypeArguments().isEmpty() == false) {
				type = declaringType.getActualTypeArguments().get(0);
			} else {
				type = originalType;
			}

			if ((stratumPos == 10) || ((structuralStratum != MAP) && ((structuralStratum != LIST) || (isTypeIsProtected(type) != false)))) {
				subItems = null;
			} else {
				subItems = getDtoContent(type, stratumPos + 1);
			}
		}

		private boolean isTypeIsProtected(final CtTypeReference<?> type) {
			if (type == null || type.getPackage() == null || type.getPackage().getQualifiedName() == null) {
				return true;
			}
			return StringUtils.startsWithAny(type.getPackage().getQualifiedName(),
			        "org.springframework", "java", "com.sun", "sun");
		}

		private void setLastItem(final boolean lastItem) {
			this.lastItem = lastItem;
		}

		public boolean isLastItem() {
			return lastItem;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type.getSimpleName();
		}

		public int getStratumPos() {
			return stratumPos;
		}

		public StructuralStratum getStructuralStratum() {
			return structuralStratum;
		}

		public List<DtoItem> getSubItems() {
			return subItems;
		}

		private Optional<CtTypeReference<?>> extractFirstTypeArguments(final CtTypeReference<?> ref) {
			final var typeArgs = ref.getActualTypeArguments();
			if (typeArgs.size() != 1) {
				return Optional.empty();
			}
			return Optional.ofNullable(typeArgs.get(0));
		}

	}

	private static String extractName(final CtExecutableReference<?> eref) {
		return Optional.ofNullable(eref.getAnnotation(JsonProperty.class))
		        .map(JsonProperty::value)
		        .orElseGet(() -> {
			        final var simpleName = eref.getSimpleName();
			        var w = 3;
			        if (simpleName.startsWith("is")) {
				        w = 2;
			        }
			        return simpleName.substring(w, w + 1).toLowerCase() + simpleName.substring(w + 1);
		        });
	}

	private Stream<DtoItem> getResponseDtoContent(final CtTypeReference<?> declaringType, final int stratumPos) {
		final var superClass = declaringType.getSuperclass();
		if (superClass != null && representationModelType.isSubtypeOf(superClass)) {
			return Stream.empty();
		}
		return declaringType.getAllExecutables().stream()
		        .filter(e -> {
			        if (e.isConstructor()) {
				        return false;
			        }
			        final var method = e.getActualMethod();
			        if (e.getSimpleName().startsWith("get") == false
			            && e.getSimpleName().startsWith("is") == false
			            || method.getReturnType() == null
			            || method.getParameterCount() != 0) {
				        return false;
			        }
			        final var modifiers = method.getModifiers();
			        return method.isDefault() == false
			               && Modifier.isNative(modifiers) == false
			               && Modifier.isPublic(modifiers)
			               && Modifier.isStatic(modifiers) == false
			               && method.getDeclaringClass().equals(Object.class) == false
			               && method.isAnnotationPresent(JsonIgnore.class) == false;
		        })
		        .map(eref -> new DtoItem(declaringType, extractName(eref), eref.getType(), stratumPos));
	}

	private Stream<DtoItem> getRequestDtoContent(final CtTypeReference<?> declaringType, final int stratumPos) {
		final var usableConstructors = declaringType.getAllExecutables().stream()
		        .filter(CtExecutableReference::isConstructor)
		        .map(CtExecutableReference::getActualConstructor)
		        .filter(c -> Modifier.isPublic(c.getModifiers()))
		        .filter(c -> c.isAnnotationPresent(JsonIgnore.class) == false)
		        .collect(Collectors.toUnmodifiableList());

		if (usableConstructors.isEmpty() || usableConstructors.stream().anyMatch(c -> c.getParameterCount() == 0)) {
			/**
			 * No parameterized constructors
			 */
			return declaringType.getAllExecutables().stream()
			        .filter(exec -> exec.isConstructor() == false)
			        .filter(e -> {
				        final var method = e.getActualMethod();
				        if (e.getSimpleName().startsWith("set") == false
				            || method.getParameterCount() != 1) {
					        return false;
				        }
				        final var modifiers = method.getModifiers();
				        return method.isDefault() == false
				               && Modifier.isNative(modifiers) == false
				               && Modifier.isPublic(modifiers)
				               && Modifier.isStatic(modifiers) == false
				               && method.isAnnotationPresent(JsonIgnore.class) == false;
			        })
			        .map(eref -> new DtoItem(declaringType, extractName(eref),
			                eref.getParameters().get(0), stratumPos));
		} else if (usableConstructors.size() > 1) {
			log.error("Can't use {} as DTO body request because there are too many ({}) accessible constructors",
			        declaringType.getQualifiedName(), usableConstructors.size());
			return Stream.empty();
		} else {
			/**
			 * Only one parameterized constructor
			 */
			return Arrays.stream(usableConstructors.get(0).getParameters())
			        .filter(p -> p.isAnnotationPresent(JsonIgnore.class) == false)
			        .filter(p -> p.isVarArgs() == false)
			        .map(p -> {
				        final var name = Optional.ofNullable(p.getAnnotation(JsonProperty.class))
				                .map(JsonProperty::value)
				                .orElseGet(() -> {
					                if (p.isNamePresent() == false) {
						                return declaringType.getQualifiedName() + "(" + p.getName() + ")";
					                } else {
						                return p.getName();
					                }
				                });
				        return new DtoItem(declaringType, name,
				                typeFactory.get(p.getType()).getReference(), stratumPos);
			        });
		}
	}

	public List<DtoItem> getDtoContent(final CtTypeReference<?> declaringType, final int stratumPos) {
		Stream<DtoItem> dto;
		if (isResponseDto) {
			dto = getResponseDtoContent(declaringType, stratumPos);
		} else {
			dto = getRequestDtoContent(declaringType, stratumPos);
		}
		final var result = dto.sorted((l, r) -> l.getName().compareTo(r.getName()))
		        .collect(Collectors.toUnmodifiableList());

		if (result.isEmpty() == false) {
			result.get(result.size() - 1).setLastItem(true);
		}
		return result;
	}

	public List<DtoItem> getDtoContent() {
		return getDtoContent(declaringType, 0);
	}
}
