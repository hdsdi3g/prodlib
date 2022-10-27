/*
 * This file is part of jobkit.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class SupervisableServiceSupplierTest {
	static Faker faker = Faker.instance();

	SupervisableServiceSupplier sss;

	@Mock
	SupervisableManager supervisableManager;
	@Mock
	Supervisable supervisable;
	@Mock
	Exception exception;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		sss = new SupervisableServiceSupplier(supervisableManager);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(supervisableManager, supervisable, exception);
		sss.end(supervisable, Optional.empty());
	}

	@Test
	void testCreateAndStart() {
		final var name = faker.numerify("name###");
		final var managerName = faker.numerify("managerName###");

		final var supervisable = sss.createAndStart(name);
		assertNotNull(supervisable);
		supervisable.resultDone();

		final var oEvent = supervisable.getEndEvent(Optional.empty(), managerName);
		assertTrue(oEvent.isPresent());
		final var event = oEvent.get();
		assertEquals(name, event.jobName());
		assertEquals(Thread.currentThread().getName(), event.spoolName());
		assertEquals(managerName, event.managerName());
		assertNotNull(Supervisable.getSupervisable());
		assertEquals(supervisable, Supervisable.getSupervisable());
		assertNull(event.endDate());
		assertNotNull(event.startDate());
	}

	@Test
	void testEnd_noError() {
		sss.end(supervisable, Optional.empty());
		verify(supervisable, times(1)).end();
		assertThrows(IllegalThreadStateException.class, Supervisable::getSupervisable);
	}

	@Test
	void testEnd_error() {
		sss.end(supervisable, Optional.ofNullable(exception));
		verify(supervisable, times(1)).end(exception);
		assertThrows(IllegalThreadStateException.class, Supervisable::getSupervisable);
	}
}