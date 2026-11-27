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

import jakarta.inject.Inject;

import java.util.Set;

/**
 * Applies all registered {@link EventDefinitionNormalizer}s in turn.
 */
public class EventDefinitionNormalizers {
    private final Set<EventDefinitionNormalizer> normalizers;

    @Inject
    public EventDefinitionNormalizers(Set<EventDefinitionNormalizer> normalizers) {
        this.normalizers = normalizers;
    }

    public static EventDefinitionNormalizers none() {
        return new EventDefinitionNormalizers(Set.of());
    }

    public EventDefinitionDto normalize(EventDefinitionDto eventDefinition) {
        EventDefinitionDto result = eventDefinition;
        for (final EventDefinitionNormalizer normalizer : normalizers) {
            result = normalizer.normalize(result);
        }
        return result;
    }
}
