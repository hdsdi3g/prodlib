/*
 * This file is part of jobkit-engine.
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
package tv.hd3g.jobkit.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class AtomicComputeReferenceTest {

	AtomicComputeReference<Item> acr;

	interface Item {
		Object getContent();

		void setContent();
	}

	@Mock
	Item item;
	@Mock
	Item item2;
	@Mock
	Function<Item, Object> process;
	@Mock
	Predicate<Item> predicate;
	@Mock
	UnaryOperator<Item> replace;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		acr = new AtomicComputeReference<>();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(item, item2, process, predicate, replace);
	}

	@Test
	void testGetSet() {
		assertNull(acr.get());
		acr.set(item);
		assertEquals(item, acr.get());
	}

	@Test
	void testSet() {
		acr.set(item);
		acr.set(null);
		assertNull(acr.get());
	}

	@Test
	void testSetAnd() {
		acr.setAnd(item, i -> {
			assertEquals(item, i);
			assertEquals(item, acr.get());
		});
		assertEquals(item, acr.get());
	}

	@Test
	void testIsSet() {
		assertFalse(acr.isSet());
		acr.set(item);
		assertTrue(acr.isSet());
	}

	@Test
	void testReset() {
		acr.set(item);
		assertEquals(item, acr.reset());
		assertNull(acr.get());
	}

	@Test
	void testCompute_null() {
		assertNull(acr.compute(process));
	}

	@Test
	void testCompute() {
		final var ref = new AtomicReference<Item>();

		acr.set(item);
		assertEquals(item2, acr.compute(i -> {
			ref.set(i);
			return item2;
		}));
		assertEquals(item, ref.get());
	}

	@Test
	void testComputePredicate_null() {
		assertFalse(acr.computePredicate(predicate));
	}

	@Test
	void testComputePredicate() {
		final var ref = new AtomicReference<Item>();

		acr.set(item);
		assertTrue(acr.computePredicate(i -> {
			ref.set(i);
			return true;
		}));
		assertEquals(item, ref.get());
	}

	@Test
	void testReplace() {
		acr.set(item);
		when(replace.apply(item)).thenReturn(item2);
		acr.replace(replace);
		verify(replace, times(1)).apply(item);
		assertEquals(item2, acr.get());
	}

}
