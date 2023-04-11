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
package tv.hd3g.jobkit.watchfolder.mod.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import net.datafaker.Faker;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;

@SpringBootTest
class WatchedFileScannerSupplierTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Autowired
	WatchedFileScannerSupplier s;
	@Value("${watchfolder.maxDeep:10}")
	int maxDeep;

	@Mock
	ObservedFolder observedFolder;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@Test
	void testCreate() {
		when(observedFolder.isRecursive()).thenReturn(true);
		final var result = s.create(observedFolder);
		assertNotNull(result);
		assertEquals(maxDeep, result.getMaxDeep());
		verify(observedFolder, times(1)).isRecursive();
	}

}
