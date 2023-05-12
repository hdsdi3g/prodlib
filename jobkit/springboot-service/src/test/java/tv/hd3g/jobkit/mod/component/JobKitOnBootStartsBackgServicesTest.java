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

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tv.hd3g.jobkit.engine.JobKitEngine;

@SpringBootTest
class JobKitOnBootStartsBackgServicesTest {

	@MockBean
	JobKitEngine jobKitEngine;
	@Autowired
	JobKitOnBootStartsBackgServices onBoot;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(jobKitEngine);
	}

	@Test
	void test() throws Exception {
		verify(jobKitEngine, only()).onApplicationReadyRunBackgroundServices();
	}
}
