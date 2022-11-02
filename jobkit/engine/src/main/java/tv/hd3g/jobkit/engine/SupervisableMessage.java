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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record SupervisableMessage(String code,
								  String defaultResult,
								  List<String> vars) {

	public SupervisableMessage(final String code,
							   final String defaultResult,
							   final List<String> vars) {
		this.code = Objects.requireNonNull(code, "\"code\" can't to be null");
		this.defaultResult = Objects.requireNonNull(defaultResult, "\"defaultResult\" can't to be null");
		this.vars = Objects.requireNonNull(vars, "\"vars\" can't to be null");
	}

	public SupervisableMessage(final String code,
							   final String defaultResult,
							   final Object[] vars) {
		this(code, defaultResult, extractVars(vars));
		checkDefaultResult();
	}

	private static List<String> extractVars(final Object[] vars) {
		if (vars != null && vars.length > 0) {
			return Stream.of(vars)
					.map(v -> {
						if (v instanceof final String strValue) {
							return strValue;
						}
						return String.valueOf(v);
					})
					.toList();
		} else {
			return List.of();
		}
	}

	public String[] getVarsArray() {
		return vars.toArray(new String[] {});
	}

	private void checkDefaultResult() {
		if (defaultResult.contains("{}")) {
			throw new IllegalArgumentException("Never use \"{}\" as defaultResult on \""
											   + code + "\", always add a number like {0}");
		}
	}

}
