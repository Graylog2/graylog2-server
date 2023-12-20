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

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Resolves dependencies between events
 */
public interface EventResolver {
    /**
     * Returns IDs of dependent event definitions
     *
     * @param definitionId an event definition ID
     * @return the dependent event definitions
     */
    @NotNull
    List<EventDefinitionDto> dependentEvents(String definitionId);
}
