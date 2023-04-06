/*
 * This file is part of jsconfig.
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
package tv.hd3g.commons.jsconfig.mod;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tv.hd3g.commons.jsconfig.JSUpdateWatcher;
import tv.hd3g.commons.jsconfig.mod.component.ContextBuilderProvider;

@SpringBootTest
class JSConfigSetupTest {

	@Autowired
	JSConfigSetup j;

	@MockBean
	JSConfigConfig config;
	@MockBean
	ContextBuilderProvider contextBuilderProvider;
	@MockBean
	JSUpdateWatcher jsUpdateWatcher;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(config, contextBuilderProvider, jsUpdateWatcher);
	}

	@Test
	void testGetJSConfigContextBindingsProvider() {
		assertNotNull(j.getJSConfigContextBindingsProvider(
				config, contextBuilderProvider, jsUpdateWatcher));
	}

	@Test
	void testGetJSUpdateWatcher() {
		assertNotNull(j.getJSUpdateWatcher(config));
		verify(config, times(1)).getFileDirSrc();
		verify(config, times(1)).getFileSrc();
		verify(config, times(1)).getDirSrc();
	}

}
