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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package tv.hd3g.jobkit.engine.watchdog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class WatchableSpoolJobStateTest {

	WatchableSpoolJobState w;

	@Test
	void testGetRunTime() {
		final var now = System.currentTimeMillis() - 1;
		w = new WatchableSpoolJobState(new Date(), null, 0, Optional.empty(), Optional.ofNullable(now));
		assertTrue(w.getRunTime().get().toMillis() > 0l);
		assertTrue(w.getRunTime().get().toMillis() < 100l);
	}

	@Test
	void testGetRunTime_empty() {
		w = new WatchableSpoolJobState(new Date(), null, 0, Optional.empty(), Optional.empty());
		assertTrue(w.getRunTime().isEmpty());
	}

}
