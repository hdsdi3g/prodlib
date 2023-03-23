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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tv.hd3g.commons.jsconfig.JSConfigRunner;

@Configuration
public class JSConfigSetup {

	@Bean
	JSConfigRunner getJSConfigContextBindingsProvider(final JSConfigConfig config) {
		return new JSConfigRunner(config);
	}

}
