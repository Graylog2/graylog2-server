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
package org.graylog2.metrics.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds metric values per entity: entity ID → field name → value.
 */
public record EntityMetricValues(Map<String, Map<String, Object>> values) {

    public Map<String, Map<String, Object>> toMap() {
        return values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Map<String, Object>> values = new HashMap<>();

        public void put(String entityId, String field, Object value) {
            values.computeIfAbsent(entityId, k -> new HashMap<>()).put(field, value);
        }

        public EntityMetricValues build() {
            return new EntityMetricValues(values.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> Collections.unmodifiableMap(e.getValue())
                    )));
        }
    }
}
