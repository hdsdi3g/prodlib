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

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public interface TimeOutTrait {

	Duration getTimeout();

	default void whileToTimeout(final BooleanSupplier condition,
								final Runnable onTimeout) throws InterruptedIOException {
		if (condition.getAsBoolean() == false) {
			return;
		}
		final var timeoutDate = System.currentTimeMillis() + getTimeout().toMillis();

		while (condition.getAsBoolean()) {
			Thread.onSpinWait();
			if (System.currentTimeMillis() > timeoutDate) {
				onTimeout.run();
				throw new InterruptedIOException();
			}
		}
	}

}
