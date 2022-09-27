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
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;

class NotificationGroupTest {

	static Faker faker = Faker.instance();

	NotificationGroup ng;

	Locale locale;
	@Mock
	Set<String> addrList;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		locale = Locale.ROOT;
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(addrList);
	}

	@Test
	void test_ok() {
		when(addrList.isEmpty()).thenReturn(false);
		ng = new NotificationGroup(addrList, locale);
		assertEquals(addrList, ng.addrList());
		assertEquals(locale, ng.lang());
		verify(addrList, atMostOnce()).isEmpty();
	}

	@Test
	void test_empty() {
		when(addrList.isEmpty()).thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> new NotificationGroup(addrList, locale));
		verify(addrList, atMostOnce()).isEmpty();
	}

}
