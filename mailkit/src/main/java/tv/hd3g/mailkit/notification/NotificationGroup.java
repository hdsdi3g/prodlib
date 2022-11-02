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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.properties.bind.ConstructorBinding;

public record NotificationGroup(Set<String> addrList,
								Locale lang) {

	@ConstructorBinding
	public NotificationGroup(final Set<String> addrList,
							 final Locale lang) {
		this.addrList = Objects.requireNonNull(addrList, "\"addrListaddrList\" can't to be null");
		this.lang = Objects.requireNonNull(lang, "\"lang\" can't to be null");

		if (addrList.isEmpty()) {
			throw new IllegalArgumentException("addrList can't be empty");
		}
	}

}
