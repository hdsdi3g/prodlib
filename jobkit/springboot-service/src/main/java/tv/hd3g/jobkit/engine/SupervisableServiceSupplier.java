/*
 * This file is part of jobkit.
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

import static tv.hd3g.jobkit.engine.Supervisable.manuallyRegistedSupervisables;

import java.util.Optional;

public class SupervisableServiceSupplier {
	private final SupervisableManager supervisableManager;

	public SupervisableServiceSupplier(final SupervisableManager supervisableManager) {
		this.supervisableManager = supervisableManager;
	}

	/**
	 * ALWAYS CALL end() after createAndStart() to release static internal Supervisable
	 */
	public Supervisable createAndStart(final String jobName) {
		final var supervisable = new Supervisable(
				Thread.currentThread().getName(),
				jobName,
				supervisableManager);
		manuallyRegistedSupervisables.set(supervisable);
		supervisable.start();
		return supervisable;
	}

	public void end(final Supervisable supervisable, final Optional<Exception> error) {
		error.ifPresentOrElse(supervisable::end, supervisable::end);
		manuallyRegistedSupervisables.remove();
	}

}
