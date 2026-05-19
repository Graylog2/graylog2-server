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
package org.graylog2.metrics.entity.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Result of a cache lookup. Contains fresh cached values and identifies which fields
 * are stale or missing per entity.
 */
public record CacheResult(
        Map<String, Map<String, Object>> freshFields,
        Map<String, Set<String>> staleFields
) {
    public Set<String> staleEntityIds() {
        return staleFields.keySet();
    }

    public Set<String> staleEntityIdsForField(String field) {
        final Set<String> staleIds = new HashSet<>();
        for (final var entry : staleFields.entrySet()) {
            if (entry.getValue().contains(field)) {
                staleIds.add(entry.getKey());
            }
        }
        return staleIds;
    }

    public static Builder builder(Collection<String> entityIds, Collection<String> fields) {
        return new Builder(entityIds, fields);
    }

    public static class Builder {
        private final Set<String> allEntityIds;
        private final Collection<String> allFields;
        private final Set<String> foundIds = new HashSet<>();
        private final Map<String, Map<String, Object>> freshFields = new HashMap<>();
        private final Map<String, Set<String>> staleFields = new HashMap<>();

        private Builder(Collection<String> entityIds, Collection<String> fields) {
            this.allEntityIds = new HashSet<>(entityIds);
            this.allFields = fields;
        }

        public void markFound(String entityId) {
            foundIds.add(entityId);
        }

        public void addFresh(String entityId, String field, Object value) {
            freshFields.computeIfAbsent(entityId, k -> new HashMap<>()).put(field, value);
        }

        public void addStale(String entityId, String field) {
            staleFields.computeIfAbsent(entityId, k -> new HashSet<>()).add(field);
        }

        public CacheResult build() {
            for (final String entityId : allEntityIds) {
                if (!foundIds.contains(entityId)) {
                    staleFields.put(entityId, new HashSet<>(allFields));
                }
            }
            return new CacheResult(freshFields, staleFields);
        }
    }
}
