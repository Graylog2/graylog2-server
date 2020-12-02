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
package org.graylog.events.processor.storage;

import static java.util.Objects.requireNonNull;

/**
 * This indicates an error in an {@link EventStorageHandler}.
 */
public class EventStorageHandlerException extends Exception {
    private final EventStorageHandler.Config handlerConfig;

    public EventStorageHandlerException(String message, EventStorageHandler.Config handlerConfig) {
        super(message);
        this.handlerConfig = requireNonNull(handlerConfig, "handlerConfig cannot be null");
    }

    public EventStorageHandlerException(String message, EventStorageHandler.Config handlerConfig, Throwable cause) {
        super(message, cause);
        this.handlerConfig = requireNonNull(handlerConfig, "handlerConfig cannot be null");
    }

    /**
     * Returns the {@link EventStorageHandler.Config} for the storage handler that failed.
     *
     * @return failing storage handler config
     */
    public EventStorageHandler.Config getHandlerConfig() {
        return handlerConfig;
    }
}
