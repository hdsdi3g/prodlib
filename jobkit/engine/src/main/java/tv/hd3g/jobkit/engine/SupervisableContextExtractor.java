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
package tv.hd3g.jobkit.engine;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.NullNode;

public class SupervisableContextExtractor {
	private static Logger log = LogManager.getLogger();

	private final SupervisableSerializer serializer;
	private final SupervisableEndEvent event;

	public SupervisableContextExtractor(final SupervisableSerializer serializer, final SupervisableEndEvent event) {
		this.serializer = Objects.requireNonNull(serializer, "\"serializer\" can't to be null");
		this.event = Objects.requireNonNull(event, "\"event\" can't to be null");
	}

	public <T> T getBusinessObject(final Class<T> type) {
		if (event.context() == null) {
			return null;
		}
		try {
			return serializer.getBusinessObject(event.context(), type);
		} catch (final JsonProcessingException e) {
			log.warn("Can't extract Json from {}, as type {}", event.context(), type, e);
			return null;
		}
	}

	public <T> T getBusinessObject(final String key, final Class<T> type) {
		if (event.context() == null) {
			return null;
		}
		final var node = event.context().get(key);
		if (node == null || NullNode.instance.equals(node)) {
			return null;
		}
		try {
			return serializer.getBusinessObject(node, type);
		} catch (final JsonProcessingException e) {
			log.warn("Can't extract Json from {}: {}, as type {}", key, node, type, e);
			return null;
		}
	}

}
