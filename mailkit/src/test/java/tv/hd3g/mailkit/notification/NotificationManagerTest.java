/*
 * This file is part of mailkit.
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
package tv.hd3g.mailkit.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;
import tv.hd3g.jobkit.engine.SupervisableEventRegister;
import tv.hd3g.jobkit.engine.SupervisableOnEndEventConsumer;

class NotificationManagerTest {

	static Faker faker = Faker.instance();

	NotificationManager n;

	@Mock
	SupervisableEventRegister supervisable;
	@Mock
	SupervisableEndEvent event;
	@Mock
	NotificationRouter router;
	@Captor
	ArgumentCaptor<SupervisableOnEndEventConsumer> consumerCaptor;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		n = new NotificationManager();
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(supervisable, event, router);
	}

	@Test
	void registerSupervisableManager_noRouter() {
		assertThrows(IllegalStateException.class, () -> n.register(supervisable));
	}

	@Test
	void registerSupervisableManager() {
		n.register(router);
		assertEquals(n, n.register(supervisable));

		verify(supervisable, times(1)).registerOnEndEventConsumer(consumerCaptor.capture());
		consumerCaptor.getValue().afterProcess(event);
		verify(router, times(1)).send(event);
	}

}
