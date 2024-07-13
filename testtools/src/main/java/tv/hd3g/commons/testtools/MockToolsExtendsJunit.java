/*
 * This file is part of testtools.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package tv.hd3g.commons.testtools;

import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.Mockito;

import net.datafaker.Faker;

/**
 * Used @ExtendWith(MockToolsExtendsJunit.class)
 */
public class MockToolsExtendsJunit implements BeforeEachCallback, AfterEachCallback {
	static Faker faker = net.datafaker.Faker.instance();

	Object getFake(final Field field, final Class<?> fromClass) {
		final Class<?> type = field.getType();
		final var name = field.getName();

		final var fakeA = field.getAnnotation(Fake.class);
		if (fakeA.min() > fakeA.max()) {
			throw new ArithmeticException("[" + name + "] Annotation min can't be more than max");
		}

		if (type.isAssignableFrom(String.class)) {
			return faker.numerify(name + "#####");
		} else if (type.isAssignableFrom(File.class)) {
			return new File(faker.numerify(name + "#####"));
		} else if (type.isEnum()) {
			return faker.options().option(type.getEnumConstants());
		} else if (type.isAssignableFrom(Integer.TYPE)) {
			if (fakeA.min() == fakeA.max()) {
				return faker.random().nextInt();
			}
			return faker.random().nextInt((int) fakeA.min(), (int) fakeA.max());
		} else if (type.isAssignableFrom(Long.TYPE)) {
			if (fakeA.min() == fakeA.max()) {
				return faker.random().nextLong();
			}
			return faker.random().nextLong(fakeA.min(), fakeA.max());
		} else if (type.isAssignableFrom(Double.TYPE)) {
			if (fakeA.min() == fakeA.max()) {
				return faker.random().nextDouble();
			}
			return faker.random().nextDouble(fakeA.min(), fakeA.max());
		} else if (type.isAssignableFrom(Float.TYPE)) {
			if (fakeA.min() == fakeA.max()) {
				return faker.random().nextFloat();
			}
			return faker.random().getRandomInternal().nextFloat(fakeA.min(), fakeA.max());
		} else if (type.isAssignableFrom(Boolean.TYPE)) {
			return faker.random().nextBoolean();
		}

		throw new IllegalArgumentException("Can't manage this type: "
										   + type.getName()
										   + " on field [" + fromClass.getName() + "." + name + "]");
	}

	void apply(final Object instance) {
		try {
			openMocks(instance).close();
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}

		final var testClass = instance.getClass();

		final Consumer<Field> setValue = f -> {
			try {
				f.set(instance, getFake(f, instance.getClass()));// NOSONAR S3011
			} catch (final IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		};

		Stream.of(testClass.getDeclaredFields())
				.filter(f -> f.getAnnotation(Fake.class) != null)
				.peek(f -> f.setAccessible(true))// NOSONAR S3011
				.forEach(setValue);
	}

	void check(final Object instance) {
		Stream.of(instance.getClass().getDeclaredFields())
				.filter(f -> f.getAnnotation(Mock.class) != null)
				.peek(f -> f.setAccessible(true))// NOSONAR S3011
				.map(f -> {
					try {
						return f.get(instance);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				})
				.forEach(Mockito::verifyNoMoreInteractions);
	}

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		context.getRequiredTestInstances()
				.getAllInstances()
				.forEach(this::apply);

	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		context.getRequiredTestInstances()
				.getAllInstances()
				.forEach(this::check);
	}

}
