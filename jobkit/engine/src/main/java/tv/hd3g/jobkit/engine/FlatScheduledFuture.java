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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.jobkit.engine;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
class FlatScheduledFuture implements ScheduledFuture<Void> {
	private final Runnable run;

	FlatScheduledFuture(final Runnable run) {
		this.run = run;
	}

	void run() {
		run.run();
	}

	@Override
	public long getDelay(final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(final Delayed o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void get(final long timeout,
					final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException();
	}

}
