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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.engine.RunnableWithException.fromRunnable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class RunnableWithExceptionTest {

	RunnableWithException r;

	@Mock
	Runnable run;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		r = () -> {
			run.run();
		};
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(run);

	}

	@Test
	void testToRunnable() {
		final var result = r.toRunnable();
		assertNotNull(result);
		verifyNoInteractions(run);

		result.run();
		verify(run, times(1)).run();
	}

	@Test
	void testToRunnable_error() {
		doThrow(new RuntimeException()).when(run).run();
		final var result = r.toRunnable();

		assertThrows(IllegalStateException.class, () -> result.run());
		verify(run, times(1)).run();
	}

	@Test
	void testFromRunnable() throws Exception {
		r = fromRunnable(run);

		r.run();
		verify(run, times(1)).run();
	}

}
