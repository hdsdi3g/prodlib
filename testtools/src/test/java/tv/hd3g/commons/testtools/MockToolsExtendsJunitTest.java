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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.exceptions.verification.NoInteractionsWanted;

@ExtendWith(MockToolsExtendsJunit.class)
class MockToolsExtendsJunitTest {

	@Mock
	ExtensionContext context;
	@Mock
	TestInstances testInstances;
	@Mock
	List<Object> listObjects;

	MockToolsExtendsJunit m;

	@BeforeEach
	void init() {
		m = new MockToolsExtendsJunit();
	}

	@Captor
	ArgumentCaptor<Consumer<Object>> consumerObjectCaptor;

	@Nested
	class BeforeAfterEach {

		@BeforeEach
		void init() {
			when(context.getRequiredTestInstances()).thenReturn(testInstances);
			when(testInstances.getAllInstances()).thenReturn(listObjects);
		}

		@AfterEach
		void ends() {
			verify(context, times(1)).getRequiredTestInstances();
			verify(testInstances, times(1)).getAllInstances();
			verify(listObjects, times(1)).forEach(any());
		}

		@Test
		void testBefore() throws Exception {// NOSONAR S2699
			m.beforeEach(context);
		}

		@Test
		void testAfter() throws Exception {// NOSONAR S2699
			m.afterEach(context);
		}
	}

	@Test
	void testApply() {
		assertTrue(mockingDetails(context).isMock());
	}

	@Test
	void testCheck() {
		context.getDisplayName();
		assertThrows(NoInteractionsWanted.class, () -> m.check(this));
		verify(context, times(1)).getDisplayName();
	}

	@Fake
	String fString;
	@Fake
	File fFile;
	@Fake
	E fEnum;

	enum E {
		A,
		B,
		C,
		D;
	}

	@Fake
	int fInt;
	@Fake
	long fLong;
	@Fake
	double fDouble;
	@Fake
	float fFloat;
	@Fake
	boolean fBoolean;

	@Fake(min = -100, max = -5)
	int fNegBoundInt;
	@Fake(min = -100, max = -5)
	long fNegBoundLong;
	@Fake(min = -100, max = -5)
	double fNegBoundDouble;
	@Fake(min = -100, max = -5)
	float fNegBoundFloat;

	@Fake(min = 1, max = 50)
	int fPosBoundInt;
	@Fake(min = 1, max = 50)
	long fPosBoundLong;
	@Fake(min = 1, max = 50)
	double fPosBoundDouble;
	@Fake(min = 1, max = 50)
	float fPosBoundFloat;

	@Test
	void testFake() {
		assertThat(fString).isNotBlank();
		assertThat(fFile).doesNotExist();
		assertThat(fEnum).isNotNull();

		assertThat(fInt).isNotZero();
		assertThat(fLong).isNotZero();
		assertThat(fDouble).isNotZero();
		assertThat(fFloat).isNotZero();

		assertThat(fNegBoundInt).isBetween(-100, -5);
		assertThat(fNegBoundLong).isBetween(-100l, -5l);
		assertThat(fNegBoundDouble).isBetween(-100d, -5d);
		assertThat(fNegBoundFloat).isBetween(-100f, -5f);

		assertThat(fPosBoundInt).isBetween(1, 50);
		assertThat(fPosBoundLong).isBetween(1l, 50l);
		assertThat(fPosBoundDouble).isBetween(1d, 50d);
		assertThat(fPosBoundFloat).isBetween(1f, 50f);
	}

	static class TestFakeError {
		@Fake(min = -5, max = -10)
		int value;
	}

	@Test
	void testFakeError() {
		final var tfe = new TestFakeError();
		assertThrows(ArithmeticException.class, () -> m.apply(tfe));
	}

	static class TestFakeNonManaged {
		@Fake
		System value;
	}

	@Test
	void testFakeNonManaged() {
		final var tfe = new TestFakeNonManaged();
		assertThrows(IllegalArgumentException.class, () -> m.apply(tfe));
	}

}
