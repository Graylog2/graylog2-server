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

/**
 * Represents an operation that accepts a single input argument and returns no result.
 * <p>
 * The main difference to {@link java.util.function.Consumer} is that {@link #accept(Object)} declares to throw a
 * {@link EventProcessorException}.
 *
 * @param <T> the type of the input to the operation
 */
public interface EventConsumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param events the input
     * @throws EventProcessorException thrown when event processing fails
     */
    void accept(T events) throws EventProcessorException;
}
