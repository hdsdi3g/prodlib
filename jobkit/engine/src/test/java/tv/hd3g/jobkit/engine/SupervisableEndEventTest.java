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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.INTERNAL_STATE_CHANGE;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.SECURITY;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.TRIVIAL;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.URGENT;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.JsonNode;

import net.datafaker.Faker;

class SupervisableEndEventTest {
	static Faker faker = Faker.instance();

	SupervisableEndEvent event;

	String spoolName;
	String jobName;
	String typeName;
	String managerName;
	Set<SupervisableEventMark> marks;

	@Mock
	JsonNode context;
	@Mock
	Date creationDate;
	@Mock
	Date startDate;
	@Mock
	Date endDate;
	@Mock
	List<SupervisableStep> steps;
	@Mock
	SupervisableResult result;
	@Mock
	Exception error;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		// assertTrue(MockUtil.isMock());
		// MockUtil.resetMock(toolRunner);
		// @MockBean
		// @Captor ArgumentCaptor<>
		// Mockito.doThrow(new Exception()).when();
		spoolName = faker.numerify("spoolName###");
		jobName = faker.numerify("jobName###");
		typeName = faker.numerify("typeName###");
		managerName = faker.numerify("managerName###");
		marks = new HashSet<>();

		event = new SupervisableEndEvent(
				spoolName,
				jobName,
				typeName,
				managerName,
				context,
				creationDate,
				startDate,
				endDate,
				steps,
				result,
				error,
				marks);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(
				context,
				creationDate,
				startDate,
				endDate,
				steps,
				result,
				error);

	}

	@Test
	void testTypeName() {
		assertEquals(typeName, event.typeName());
	}

	@Test
	void testTypeName_null() {
		event = new SupervisableEndEvent(
				spoolName,
				jobName,
				null,
				managerName,
				context,
				creationDate,
				startDate,
				endDate,
				steps,
				result,
				error,
				marks);

		assertEquals("", event.typeName());
	}

	@Test
	void testIsTypeName_null() {
		assertFalse(event.isTypeName());
		assertFalse(event.isTypeName(new String[] {}));

		event = new SupervisableEndEvent(
				spoolName,
				jobName,
				null,
				managerName,
				context,
				creationDate,
				startDate,
				endDate,
				steps,
				result,
				error,
				marks);
		assertFalse(event.isTypeName());
	}

	@Test
	void testIsTypeName() {
		assertTrue(event.isTypeName(typeName, faker.camera().brand()));
		assertFalse(event.isTypeName(faker.camera().brand()));
	}

	@Test
	void testIsSecurityMarked() {
		assertFalse(event.isSecurityMarked());
		marks.add(SECURITY);
		assertTrue(event.isSecurityMarked());
	}

	@Test
	void testIsNotTrivialMarked() {
		assertTrue(event.isNotTrivialMarked());
		marks.add(TRIVIAL);
		assertFalse(event.isNotTrivialMarked());
	}

	@Test
	void testIsInternalStateChangeMarked() {
		assertFalse(event.isInternalStateChangeMarked());
		marks.add(INTERNAL_STATE_CHANGE);
		assertTrue(event.isInternalStateChangeMarked());
	}

	@Test
	void testIsUrgentMarked() {
		assertFalse(event.isUrgentMarked());
		marks.add(URGENT);
		assertTrue(event.isUrgentMarked());
	}

}
