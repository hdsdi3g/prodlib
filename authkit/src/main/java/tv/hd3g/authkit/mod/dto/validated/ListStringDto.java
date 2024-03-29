/*
 * This file is part of AuthKit.
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
package tv.hd3g.authkit.mod.dto.validated;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import tv.hd3g.authkit.utility.LogSanitizer;

/**
 * Trim and remove new lines/tabs via LogSanitizer
 */
public class ListStringDto {

	@NotNull
	private List<String> list;

	public List<String> getList() {
		return list.stream()
				.map(LogSanitizer::sanitize)
				.toList();
	}

	public void setList(final List<String> list) {
		this.list = list;
	}

}
