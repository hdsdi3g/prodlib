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
package tv.hd3g.mailkit.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

class ExceptionToStringTest {
	static Faker faker = Faker.instance();

	ExceptionToString exceptionToString;
	Exception e;

	@BeforeEach
	void init() throws Exception {
		exceptionToString = new ExceptionToString();

		final var sub0 = new FileNotFoundException(faker.medical().diseaseName());
		final var sub1 = new IllegalArgumentException(faker.medical().diseaseName(), sub0);
		final var sub2 = new RuntimeException(faker.medical().diseaseName(), sub1);
		e = new Exception(faker.medical().diseaseName(), sub2);
	}

	@Test
	void testGetStackTrace() {
		final var result = exceptionToString.getStackTrace(e);
		assertNotNull(result);
		assertTrue(result.startsWith("java.lang.Exception: " + e.getMessage() + "\r\n"));
		assertTrue(result.lines().count() > 4);
	}

	@Test
	void testGetSimpleStackTrace() {
		final var result = exceptionToString.getSimpleStackTrace(e);
		assertNotNull(result);
		assertTrue(result.startsWith("Exception: " + e.getMessage()));
		assertEquals(4, result.lines().count());
	}

	static class CircualException extends Exception {
		public CircualException() {
			super(faker.medical().diseaseName());
		}

		@Override
		public synchronized Throwable getCause() {
			/**
			 * This is evil
			 */
			return this;
		}
	}

	@Test
	void testGetSimpleStackTrace_circular() {
		final var sub0 = new CircualException();
		e = new Exception(faker.medical().diseaseName(), sub0);

		final var result = exceptionToString.getSimpleStackTrace(e);
		assertNotNull(result);
		assertTrue(result.startsWith("Exception: " + e.getMessage()));
		assertEquals(2, result.lines().count());
	}

}
