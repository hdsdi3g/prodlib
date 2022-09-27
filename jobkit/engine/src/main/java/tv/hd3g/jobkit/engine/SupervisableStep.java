/*
 * This file is part of supervisablekit.
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

import java.util.Date;
import java.util.Objects;

public record SupervisableStep(Date stepDate,
							   SupervisableMessage message,
							   StackTraceElement caller) {

	public SupervisableStep(final Date stepDate,
							final SupervisableMessage message,
							final StackTraceElement caller) {
		this.stepDate = Objects.requireNonNull(stepDate, "\"stepDate\" can't to be null");
		this.message = Objects.requireNonNull(message, "\"message\" can't to be null");
		this.caller = Objects.requireNonNull(caller, "\"caller\" can't to be null");
	}
}
