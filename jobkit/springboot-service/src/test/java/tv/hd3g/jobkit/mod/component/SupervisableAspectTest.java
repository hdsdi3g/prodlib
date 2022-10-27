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
package tv.hd3g.jobkit.mod.component;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.RunnableWithException;
import tv.hd3g.jobkit.engine.Supervisable;
import tv.hd3g.jobkit.engine.SupervisableServiceSupplier;

@SpringBootTest
class SupervisableAspectTest {

	@Autowired
	TestWithSupervisable testWithSupervisable;
	@Autowired
	JobKitEngine jobKitEngine;
	@Mock
	Supervisable supervisable;
	@MockBean
	SupervisableServiceSupplier supervisableServiceSupplier;
	@Mock
	RunnableWithException runnableWithException;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(runnableWithException, supervisableServiceSupplier, supervisable);
	}

	@Test
	void testWithDefaultName() throws Exception {
		when(supervisableServiceSupplier.createAndStart("TestWithSupervisable.aSupervisableMethod"))
				.thenReturn(supervisable);

		testWithSupervisable.aSupervisableMethod(runnableWithException);
		verify(runnableWithException, times(1)).run();
		verify(supervisableServiceSupplier, times(1)).createAndStart("TestWithSupervisable.aSupervisableMethod");
		verify(supervisableServiceSupplier, times(1)).end(supervisable, Optional.empty());
	}

	@Test
	void testWithSpecifiedName() throws Exception {
		when(supervisableServiceSupplier.createAndStart("jobkittest"))
				.thenReturn(supervisable);

		testWithSupervisable.aSupervisableMethodWithName(runnableWithException);
		verify(runnableWithException, times(1)).run();
		verify(supervisableServiceSupplier, times(1)).createAndStart("jobkittest");
		verify(supervisableServiceSupplier, times(1)).end(supervisable, Optional.empty());
	}

	@Test
	void testWithout() throws Exception {
		testWithSupervisable.aNoSupervisableMethod(() -> {
			assertThrows(IllegalThreadStateException.class, Supervisable::getSupervisable);
		});
	}

	@Test
	void testInJobKit() throws Exception {
		final var refS = new AtomicReference<Supervisable>();
		jobKitEngine.runOneShot("", "", 0, () -> {
			testWithSupervisable.aSupervisableMethodWithName(() -> {
				refS.set(Supervisable.getSupervisable());
			});
		}, e -> {
		});
		while (refS.get() != null) {
			Thread.onSpinWait();
		}
		verifyNoInteractions(supervisableServiceSupplier, supervisable);
	}

	@Test
	void testWithException() throws Exception {
		final var exception = Mockito.mock(Exception.class);
		when(supervisableServiceSupplier.createAndStart("jobkittest"))
				.thenReturn(supervisable);
		doThrow(exception).when(runnableWithException).run();

		assertThrows(Exception.class,
				() -> testWithSupervisable.aSupervisableMethodWithName(runnableWithException));
		verify(runnableWithException, times(1)).run();
		verify(supervisableServiceSupplier, times(1)).createAndStart("jobkittest");
		verify(supervisableServiceSupplier, times(1)).end(supervisable, Optional.ofNullable(exception));
		verifyNoInteractions(exception);
	}

}
