/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.processor;

import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventWithContext;
import org.graylog2.plugin.MessageSummary;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface to be implemented by event processors.
 */
public interface EventProcessor {
    interface Factory<TYPE extends EventProcessor> {
        TYPE create(EventDefinition eventDefinition);
    }

    /**
     * Creates events by using the given {@link EventFactory} and passing them to the given {@link EventConsumer}.
     *
     * @param eventFactory   the event factory to create new {@link org.graylog.events.event.Event} instances
     * @param parameters     the event processor execution parameters
     * @param eventsConsumer the event consumer
     * @throws EventProcessorException             if the execution fails
     * @throws EventProcessorPreconditionException if any preconditions are not met
     */
    void createEvents(EventFactory eventFactory, EventProcessorParameters parameters, EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException;

    /**
     * Gets all source messages for the given {@link Event} and passes them to the {@code messageConsumer}.
     *
     * @param event           the event to get all source messages for
     * @param messageConsumer the consumer that all source messages will be passed into
     * @param limit           the maximum number of messages to get
     */
    void sourceMessagesForEvent(Event event, Consumer<List<MessageSummary>> messageConsumer, long limit) throws EventProcessorException;
}
