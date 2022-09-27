/*
 * This file is part of mailkit.
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
package tv.hd3g.mailkit.notification;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExceptionToString {

	private final String newLine;

	public ExceptionToString() {
		this("\r\n");
	}

	public ExceptionToString(final String newLine) {
		this.newLine = Objects.requireNonNull(newLine, "\"newLine\" can't to be null");
	}

	public String getStackTrace(final Exception e) {
		final var cw = new CaptureWriter();
		e.printStackTrace(cw);
		return cw.lines.toString();
	}

	public String getSimpleStackTrace(final Exception e) {
		/**
		 * @see Throwable.printStackTrace(PrintStreamOrWriter s)
		 */
		final Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<>());
		final var lines = new ArrayList<String>();
		crawlInStackTrace(e, lines, "", dejaVu);
		return lines.stream().collect(Collectors.joining(newLine));
	}

	private void crawlInStackTrace(final Throwable e,
								   final List<String> lines,
								   final String prefix,
								   final Set<Throwable> dejaVu) {
		if (dejaVu.contains(e)) {
			return;
		}
		dejaVu.add(e);
		lines.add(prefix + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());

		Optional.ofNullable(e.getCause())
				.ifPresent(c -> crawlInStackTrace(c, lines, " | ", dejaVu));
	}

	private class CaptureWriter extends PrintWriter {

		StringBuilder lines;

		public CaptureWriter() {
			super(new OutputStream() {

				@Override
				public void write(final int b) throws IOException {
					throw new UnsupportedOperationException();
				}
			});
			lines = new StringBuilder();
		}

		@Override
		public void println(final Object x) {
			final var xValue = String.valueOf(x);
			if (xValue.equals(xValue.trim())) {
				lines.append(xValue);
			} else {
				lines.append(" ");
				lines.append(xValue.trim());
			}
			lines.append(newLine);
		}
	}

}
