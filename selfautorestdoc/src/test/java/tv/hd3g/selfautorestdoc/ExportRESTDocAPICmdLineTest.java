/*
 * This file is part of SelfAutoRESTDoc.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.selfautorestdoc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import tv.hd3g.selfautorestdoc.mod.ExportRESTDocAPICmdLine;

@SpringBootTest
@AutoConfigureMockMvc
class ExportRESTDocAPICmdLineTest {

	@Autowired
	private ExportRESTDocAPICmdLine exportRESTDocAPICmdLine;
	@Mock
	private ApplicationArguments args;
	@Value("${selfautorestdoc.outputfile:API.md}")
	private File outputFile;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		final var list = List.of("export-rest-doc-api", "dont-quit-after-done");
		Mockito.when(args.getNonOptionArgs()).thenReturn(list);
	}

	@Test
	void testRun() throws Exception {
		assertTrue(outputFile.delete());
		exportRESTDocAPICmdLine.run(args);

		assertTrue(outputFile.exists());
		assertTrue(outputFile.length() > 0);
		assertTrue(Files.lines(outputFile.toPath(), UTF_8).limit(1).anyMatch(l -> l.trim().equals("# REST API")));
	}

}
