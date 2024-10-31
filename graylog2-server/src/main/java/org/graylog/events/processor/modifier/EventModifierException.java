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
package org.graylog.events.processor.modifier;

/**
 * This is thrown when an {@link EventModifier} fails.
 */
public class EventModifierException extends Exception {
    private final boolean permanent;
    private final boolean skip;

    /**
     * Constructs a new exception.
     *
     * @param message   the exception message
     * @param permanent whether the error is permanent or not (should it be retried?)
     * @param skip      whether the error can be skipped and processing of the event should continue
     * @param cause     the cause
     */
    public EventModifierException(String message, boolean permanent, boolean skip, Throwable cause) {
        super(message, cause);
        this.permanent = permanent;
        this.skip = skip;
    }

    /**
     * Indicates if an error is permanent or temporary. Temporary errors could be candidates for retries.
     *
     * @return true if the error is permanent, false if temporary
     */
    public boolean isPermanent() {
        return permanent;
    }

    /**
     * Indicates if this error can be skipped by the {@link org.graylog.events.processor.EventProcessorEngine} and
     * processing of the event should continue.
     *
     * @return true if the error is skippable, false if not skippable
     */
    public boolean isSkip() {
        return skip;
    }
}
