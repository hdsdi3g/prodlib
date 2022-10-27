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

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

class SupervisableManagerTest {
	static Faker faker = Faker.instance();

	SupervisableManager s;
	String name;

	@Mock
	ObjectMapper objectMapper;
	@Mock
	Object businessObject;
	@Mock
	JsonNode context;
	@Mock
	Supervisable supervisable;
	@Mock
	Optional<Exception> oError;
	@Mock
	SupervisableOnEndEventConsumer eventConsumer;
	@Mock
	SupervisableEndEvent supervisableEndEvent;
	@Mock
	SupervisableOnEndEventConsumer eventConsumer2;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = faker.company().name();
		s = new SupervisableManager(name, objectMapper, 1);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(
				objectMapper,
				businessObject,
				context,
				supervisable,
				oError,
				eventConsumer,
				supervisableEndEvent,
				eventConsumer2);
	}

	@Test
	void testGetLifeCycle_extractContext() {
		when(objectMapper.valueToTree(businessObject)).thenReturn(context);
		final var result = s.extractContext(businessObject);
		verify(objectMapper, times(1)).valueToTree(businessObject);
		assertEquals(context, result);
	}

	@Test
	void testGetLifeCycle_extractContext_defaultObjectMapper() {
		s = new SupervisableManager(name);

		final var text = faker.artist().name();
		final var result = s.extractContext(List.of(text));
		assertEquals("[\"" + text + "\"]", result.toString());
	}

	@Test
	void testGetBusinessObject() throws JsonProcessingException, IllegalArgumentException {
		when(objectMapper.treeToValue(context, Object.class)).thenReturn(businessObject);
		final var result = s.getBusinessObject(context, Object.class);
		verify(objectMapper, times(1)).treeToValue(context, Object.class);
		assertEquals(businessObject, result);
	}

	@Test
	void testGetLifeCycle_onEnd() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.registerOnEndEventConsumer(eventConsumer);
		
		s.onEnd(supervisable, oError);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_Exception() {

		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.registerOnEndEventConsumer(eventConsumer);

		Mockito.doThrow(new IllegalStateException()).when(eventConsumer).afterProcess(supervisableEndEvent);
		s.onEnd(supervisable, oError);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_close() {
		s.close();
		s.onEnd(supervisable, oError);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testVoidSupervisableEvents() {
		assertNotNull(SupervisableManager.voidSupervisableEvents());
	}

	@Test
	void testToString() {
		assertNotNull(s.toString());
	}

	@Test
	void testCreateContextExtractor() {
		assertNotNull(s.createContextExtractor(supervisableEndEvent));
	}

	@Test
	void testGetLifeCycle_onEnd_retention() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.onEnd(supervisable, oError);
		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(1)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_limited_retention() {
		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		s.onEnd(supervisable, oError);
		s.onEnd(supervisable, oError);
		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(1)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(2)).getEndEvent(oError, name);
	}

	@Test
	void testGetLifeCycle_onEnd_non_limited_retention() {
		final var iterations = faker.random().nextInt(5, 100);
		s = new SupervisableManager(name, objectMapper, iterations);

		when(supervisable.getEndEvent(oError, name)).thenReturn(ofNullable(supervisableEndEvent));
		for (var pos = 0; pos < iterations; pos++) {
			s.onEnd(supervisable, oError);
		}
		s.onEnd(supervisable, oError);

		s.registerOnEndEventConsumer(eventConsumer);

		verify(eventConsumer, times(iterations)).afterProcess(supervisableEndEvent);
		verify(supervisable, times(iterations + 1)).getEndEvent(oError, name);

		s.registerOnEndEventConsumer(eventConsumer2);
		verify(eventConsumer2, times(iterations)).afterProcess(supervisableEndEvent);
	}

}
