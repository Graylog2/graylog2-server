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

import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.EventDefinition;

/**
 * Event modifiers can modify events before notifications and storage handlers run.
 */
public interface EventModifier {
    /**
     * Performs modification operations on the given {@link EventWithContext}.
     *
     * @param eventWithContext the event with context
     * @param eventDefinition  the event definition for the event with context
     * @throws EventModifierException when the modification fails
     */
    void accept(EventWithContext eventWithContext, EventDefinition eventDefinition) throws EventModifierException;
}
