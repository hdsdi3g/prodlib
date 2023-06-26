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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class AtomicComputeReference<T> {
	private T t;

	public synchronized T get() {
		return t;
	}

	public synchronized void set(final T t) {
		this.t = t;
	}

	public synchronized void setAnd(final T t, final Consumer<T> process) {
		this.t = t;
		process.accept(t);
	}

	public synchronized T reset() {
		final var old = t;
		t = null;
		return old;
	}

	public synchronized boolean isSet() {
		return t != null;
	}

	public synchronized <V> V compute(final Function<T, V> process) {
		if (t == null) {
			return null;
		}
		return process.apply(t);
	}

	public synchronized boolean computePredicate(final Predicate<T> process) {
		if (t == null) {
			return false;
		}
		return process.test(t);
	}

	public synchronized void replace(final UnaryOperator<T> process) {
		t = process.apply(t);
	}

}
