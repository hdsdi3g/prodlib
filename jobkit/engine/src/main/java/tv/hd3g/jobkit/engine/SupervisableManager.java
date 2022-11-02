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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupervisableManager implements SupervisableEvents {

	private final String name;
	private final ObjectMapper objectMapper;
	private final AtomicBoolean shutdown;
	private final Set<SupervisableOnEndEventConsumer> onEndEventConsumers;
	private final ArrayDeque<SupervisableEndEvent> lastEndEvents;
	private final int maxEndEventsRetention;

	public SupervisableManager(final String name, final ObjectMapper objectMapper, final int maxEndEventsRetention) {
		this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "\"objectMapper\" can't to be null");
		shutdown = new AtomicBoolean(false);
		onEndEventConsumers = Collections.synchronizedSet(new HashSet<>());
		this.maxEndEventsRetention = maxEndEventsRetention;
		lastEndEvents = new ArrayDeque<>();
	}

	public SupervisableManager(final String name) {
		this(name, new ObjectMapper(), 10);
	}

	public SupervisableContextExtractor createContextExtractor(final SupervisableEndEvent event) {
		return new SupervisableContextExtractor(this, event);
	}

	static SupervisableEvents voidSupervisableEvents() {
		/**
		 * Do nothing
		 */
		return new SupervisableEvents() {};
	}

	void close() {
		if (shutdown.get() == false) {
			log.info("Close SupervisableManager {}", name);
			shutdown.set(true);
		}
	}

	public void registerOnEndEventConsumer(final SupervisableOnEndEventConsumer onEndEventConsumer) {
		Objects.requireNonNull(onEndEventConsumer, "\"onEndEventConsumer\" can't to be null");
		onEndEventConsumers.add(onEndEventConsumer);

		synchronized (lastEndEvents) {
			lastEndEvents.forEach(onEndEventConsumer::afterProcess);
		}
	}

	@Override
	public void onEnd(final Supervisable supervisable, final Optional<Exception> oError) {
		final var oEndEvent = supervisable.getEndEvent(oError, name);
		if (oEndEvent.isEmpty()) {
			log.trace("Supervisable is empty");
			return;
		}
		final var endEvent = oEndEvent.get();

		if (shutdown.get()) {
			log.error("Can't manage event [onEnd/{}] on a closed SupervisableManager", supervisable);
			return;
		}
		log.trace("Queue end event for {}", supervisable);

		synchronized (lastEndEvents) {
			while (lastEndEvents.size() >= maxEndEventsRetention) {
				lastEndEvents.pollLast();
			}
			lastEndEvents.push(endEvent);
		}

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

	@Override
	public String toString() {
		return name;
	}

}
