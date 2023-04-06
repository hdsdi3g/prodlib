/*
 * This file is part of transfertfiles.
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
package tv.hd3g.transfertfiles;

import java.io.IOException;
import java.util.Optional;

public class InvalidURLException extends IllegalArgumentException {// TODO test

	public InvalidURLException(final String cause) {
		this(cause, null);
	}

	public InvalidURLException(final String cause, final Object url) {
		super(Optional.ofNullable(url)
				.map(u -> cause + ": " + u)
				.orElse(cause));
	}

	public InvalidURLException(final IOException e) {
		super(e.getMessage());
	}

	public static <T> T requireNonNull(final T obj, final String what, final Object url) {
		if (obj == null) {
			throw new InvalidURLException(what, url);
		}
		return obj;
	}

	public static <T> T requireNonNull(final T obj, final String what) {
		return requireNonNull(obj, what, null);
	}

}
