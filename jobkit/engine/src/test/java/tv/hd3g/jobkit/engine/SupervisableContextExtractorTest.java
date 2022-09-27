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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.datafaker.Faker;

class SupervisableContextExtractorTest {
	static Faker faker = Faker.instance();

	SupervisableContextExtractor ce;
	String key;

	@Mock
	SupervisableSerializer serializer;
	@Mock
	SupervisableEndEvent event;
	@Mock
	Object object;
	@Mock
	JsonNode context;
	@Mock
	JsonNode node;
	@Mock
	JsonProcessingException exception;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		// assertTrue(MockUtil.isMock());
		// MockUtil.resetMock(toolRunner);
		// @MockBean
		// @Captor ArgumentCaptor<>
		// Mockito.doThrow(new Exception()).when();

		ce = new SupervisableContextExtractor(serializer, event);
		key = faker.yoda().quote();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(serializer, event, object, context, node, exception);
	}

	@Test
	void testGetBusinessObjectClassOfT_noContext() {
		assertNull(ce.getBusinessObject(Object.class));
		verify(event, times(1)).context();
	}

	@Test
	void testGetBusinessObjectClassOfT() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(serializer.getBusinessObject(context, Object.class)).thenReturn(object);

		assertEquals(object, ce.getBusinessObject(Object.class));
		verify(event, atLeastOnce()).context();
		verify(serializer, times(1)).getBusinessObject(context, Object.class);
	}

	@Test
	void testGetBusinessObjectClassOfT_Error() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(serializer.getBusinessObject(context, Object.class)).thenThrow(exception);

		assertNull(ce.getBusinessObject(Object.class));
		verify(event, atLeastOnce()).context();
		verify(serializer, times(1)).getBusinessObject(context, Object.class);
	}

	@Test
	void testGetBusinessObjectStringClassOfT_noContext() {
		assertNull(ce.getBusinessObject(key, Object.class));
		verify(event, times(1)).context();
	}

	@Test
	void testGetBusinessObjectStringClassOfT() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(context.get(key)).thenReturn(node);
		when(serializer.getBusinessObject(node, Object.class)).thenReturn(object);

		assertEquals(object, ce.getBusinessObject(key, Object.class));
		verify(event, atLeastOnce()).context();
		verify(context, times(1)).get(key);
		verify(serializer, times(1)).getBusinessObject(node, Object.class);
	}

	@Test
	void testGetBusinessObjectStringClassOfT_noContextKey() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(context.get(key)).thenReturn(null);

		assertNull(ce.getBusinessObject(key, Object.class));
		verify(event, atLeastOnce()).context();
		verify(context, times(1)).get(key);
	}

	@Test
	void testGetBusinessObjectStringClassOfT_nullContextKey() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(context.get(key)).thenReturn(NullNode.instance);

		assertNull(ce.getBusinessObject(key, Object.class));
		verify(event, atLeastOnce()).context();
		verify(context, times(1)).get(key);
	}

	@Test
	void testGetBusinessObjectStringClassOfT_Error() throws JsonProcessingException {
		when(event.context()).thenReturn(context);
		when(context.get(key)).thenReturn(node);
		when(serializer.getBusinessObject(node, Object.class)).thenThrow(exception);

		assertNull(ce.getBusinessObject(key, Object.class));
		verify(event, atLeastOnce()).context();
		verify(context, times(1)).get(key);
		verify(serializer, times(1)).getBusinessObject(node, Object.class);
	}

}
