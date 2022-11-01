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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SupervisableManager {
	private static Logger log = LogManager.getLogger();

	private final String name;
	private final ObjectMapper objectMapper;
	private final LifeCycle lifeCycle;
	private final AtomicBoolean shutdown;
	private final Set<SupervisableOnEndEventConsumer> onEndEventConsumers;

	public SupervisableManager(final String name, final ObjectMapper objectMapper) {
		this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "\"objectMapper\" can't to be null");
		lifeCycle = new LifeCycle();
		shutdown = new AtomicBoolean(false);
		onEndEventConsumers = Collections.synchronizedSet(new HashSet<>());
	}

	public SupervisableManager(final String name) {
		this(name, new ObjectMapper());
	}

	public SupervisableContextExtractor createContextExtractor(final SupervisableEndEvent event) {
		return new SupervisableContextExtractor(lifeCycle, event);
	}

	static SupervisableEvents voidSupervisableEvents() {
		/**
		 * Do nothing
		 */
		return new SupervisableEvents() {};
	}

	SupervisableEvents getLifeCycle() {
		return lifeCycle;
	}

	void close() {
		log.info("Close SupervisableManager {}", name);
		shutdown.set(true);
	}

	public void registerOnEndEventConsumer(final SupervisableOnEndEventConsumer onEndEventConsumer) {
		Objects.requireNonNull(onEndEventConsumer, "\"onEndEventConsumer\" can't to be null");
		onEndEventConsumers.add(onEndEventConsumer);
	}

	class LifeCycle implements SupervisableEvents {

		private LifeCycle() {
		}

		@Override
		public void onEnd(final Supervisable supervisable, final Optional<Exception> oError) {
			if (shutdown.get()) {
				log.error("Can't manage event [onEnd/{}] on a closed SupervisableManager", supervisable);
				return;
			}
			final var endEvent = supervisable.getEndEvent(oError, name);
			log.trace("Queue end event for {}", supervisable);
			try {
				onEndEventConsumers.forEach(event -> event.afterProcess(endEvent));
			} catch (final Exception e) {
				log.error("Can't queue end event", e);
			}
		}

		@Override
		public JsonNode extractContext(final Object businessObject) {
			log.trace("Extract {} / {}...", businessObject, businessObject.getClass().getName());
			final var result = objectMapper.valueToTree(businessObject);
			log.trace("...extract result {} / {}: {}", businessObject, businessObject.getClass().getName(), result);
			return result;
		}

		@Override
		public <T> T getBusinessObject(final JsonNode context, final Class<T> type) throws JsonProcessingException {
			log.trace("Read {} / {}...", context, type.getName());
			final var result = objectMapper.treeToValue(context, type);
			log.trace("...to {}", result);
			return result;
		}

	}

	@Override
	public String toString() {
		return name;
	}

}