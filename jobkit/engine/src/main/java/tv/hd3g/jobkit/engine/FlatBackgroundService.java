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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class FlatBackgroundService extends BackgroundService {

	private final FlatScheduledFuture runReference;
	private final FlatScheduledExecutorService scheduledExecutor;
	private final RunnableWithException disableTask;

	FlatBackgroundService(final FlatScheduledExecutorService scheduledExecutor,
						  final String spoolName,
						  final Runnable task,
						  final RunnableWithException disableTask) {
		super(null, spoolName, null, scheduledExecutor, null, null, null, null);
		runReference = new FlatScheduledFuture(task);
		this.disableTask = disableTask;
		this.scheduledExecutor = scheduledExecutor;
	}

	@Override
	public synchronized BackgroundService disable() {
		scheduledExecutor.remove(runReference);
		disableTask.toRunnable().run();
		return this;
	}

	@Override
	public synchronized BackgroundService enable() {
		scheduledExecutor.add(runReference);
		return this;
	}

	@Override
	public synchronized boolean isEnabled() {
		return scheduledExecutor.contain(runReference);
	}

	@Override
	public synchronized BackgroundService setRetryAfterTimeFactor(final double retryAfterTimeFactor) {
		super.setInternalRetryAfterTimeFactor(retryAfterTimeFactor);
		return this;
	}

	@Override
	public synchronized BackgroundService setTimedInterval(final Duration duration) {
		super.setInternalTimedInterval(duration);
		return this;
	}

	@Override
	public synchronized BackgroundService setTimedInterval(final long timedInterval, final TimeUnit unit) {
		super.setInternalTimedInterval(Duration.ofMillis(unit.toMillis(timedInterval)));
		return this;
	}

	@Override
	public synchronized void runFirstOnStartup() {
		/**
		 * Not needed, not implemented
		 */
	}

}
