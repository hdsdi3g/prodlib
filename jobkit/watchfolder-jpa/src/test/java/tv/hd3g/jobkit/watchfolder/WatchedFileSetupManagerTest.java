/*
 * This file is part of jobkit-watchfolder-jpa.
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
package tv.hd3g.jobkit.watchfolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.datafaker.Faker;

class WatchedFileSetupManagerTest {
	static Faker faker = net.datafaker.Faker.instance();

	WatchedFileSetupManager m;

	@Mock
	ObservedFolder observedFolder;
	@Mock
	WatchedFileScanner scanner;
	@Mock
	WatchFolderPickupType pickUp;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		m = new WatchedFileSetupManager();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(observedFolder, scanner, pickUp);
	}

	@Test
	void testPutGet() {
		assertNull(m.get(observedFolder));
		m.put(observedFolder, scanner, pickUp);
		final var result = m.get(observedFolder);
		assertEquals(scanner, result.scanner());
		assertEquals(pickUp, result.pickUp());
	}

}
