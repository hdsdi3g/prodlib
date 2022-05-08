/*
 * This file is part of JobKit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.jobkit.mod.exception;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class JobKitRestException extends RuntimeException {

	private final int returnCode;

	public JobKitRestException(final int returnCode, final String message) {
		super(message);
		this.returnCode = returnCode;
	}

	/**
	 * SC_BAD_REQUEST
	 */
	public JobKitRestException(final String message) {
		super(message);
		returnCode = SC_BAD_REQUEST;
	}

	public int getReturnCode() {
		return returnCode;
	}

}
