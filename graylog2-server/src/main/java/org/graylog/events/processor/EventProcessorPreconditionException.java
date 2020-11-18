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
 * This gets thrown when a precondition for an event processor is not ready. The event processor should be retried
 * at a later point in time.
 */
public class EventProcessorPreconditionException extends EventProcessorException {
    public EventProcessorPreconditionException(String message, String eventDefinitionId) {
        super(message, false, eventDefinitionId);
    }

    public EventProcessorPreconditionException(String message, EventDefinition eventDefinition) {
        super(message, false, eventDefinition);
    }

    public EventProcessorPreconditionException(String message, String eventDefinitionId, EventDefinition eventDefinition) {
        super(message, false, eventDefinitionId, eventDefinition);
    }

    public EventProcessorPreconditionException(String message, String EventDefinitionId, EventDefinition eventDefinition, Throwable cause) {
        super(message, false, EventDefinitionId, eventDefinition, cause);
    }

    public EventProcessorPreconditionException(String message, EventDefinition eventDefinition, Throwable cause) {
        super(message, false, eventDefinition, cause);
    }
}
