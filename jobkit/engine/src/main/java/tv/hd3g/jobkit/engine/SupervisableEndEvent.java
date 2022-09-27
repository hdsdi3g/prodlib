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

import static tv.hd3g.jobkit.engine.SupervisableEventMark.INTERNAL_STATE_CHANGE;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.SECURITY;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.TRIVIAL;
import static tv.hd3g.jobkit.engine.SupervisableEventMark.URGENT;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

public record SupervisableEndEvent(String spoolName,
								   String jobName,
								   String typeName,
								   String managerName,
								   JsonNode context,
								   Date creationDate,
								   Date startDate,
								   Date endDate,
								   List<SupervisableStep> steps,
								   SupervisableResult result,
								   Exception error,
								   Set<SupervisableEventMark> marks) {

	/**
	 * @return never null
	 */
	public String typeName() {
		return Optional.ofNullable(typeName).orElse("");
	}

	public boolean isTypeName(final String... args) {
		if (args == null || args.length == 0 || typeName == null) {
			return false;
		}
		return Stream.of(args)
				.filter(Objects::nonNull)
				.anyMatch(typeName::equalsIgnoreCase);
	}

	public boolean isSecurityMarked() {
		return marks.contains(SECURITY);
	}

	public boolean isNotTrivialMarked() {
		return marks.contains(TRIVIAL) == false;
	}

	public boolean isInternalStateChangeMarked() {
		return marks.contains(INTERNAL_STATE_CHANGE);
	}

	public boolean isUrgentMarked() {
		return marks.contains(URGENT);
	}

}
