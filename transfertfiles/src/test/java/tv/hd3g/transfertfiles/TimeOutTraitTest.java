/*
 * This file is part of transfertfiles.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.transfertfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class TimeOutTraitTest {

	class TimeOutTraitImpl implements TimeOutTrait {

		@Override
		public Duration getTimeout() {
			return duration;
		}
	}

	@Mock
	Duration duration;
	TimeOutTrait timeOutTrait;
	AtomicInteger count;

	@BeforeEach
	public void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		timeOutTrait = new TimeOutTraitImpl();
		count = new AtomicInteger();
		when(duration.toMillis()).thenReturn(10l);
	}

	@AfterEach
	void ends() {
		Mockito.verifyNoMoreInteractions(duration);
	}

	@Test
	void testWhileToTimeout_directOk() throws InterruptedIOException {
		timeOutTrait.whileToTimeout(() -> false, () -> count.getAndIncrement());
		assertEquals(0, count.get());
	}

	@Test
	void testWhileToTimeout_ok() throws InterruptedIOException {
		final var queue = new LinkedBlockingQueue<>();
		queue.add(new Object());
		queue.add(new Object());
		queue.add(new Object());
		queue.add(new Object());

		timeOutTrait.whileToTimeout(() -> queue.poll() != null,
				() -> count.getAndIncrement());
		assertEquals(0, count.get());
		verify(duration, times(1)).toMillis();
	}

	@Test
	void testWhileToTimeout_nok() throws InterruptedIOException {
		final var now = System.currentTimeMillis();
		assertThrows(InterruptedIOException.class,
				() -> timeOutTrait.whileToTimeout(() -> true,
						() -> count.getAndIncrement()));
		assertEquals(1, count.get());
		verify(duration, times(1)).toMillis();
		assertTrue(now + 10l <= System.currentTimeMillis());
	}
}
