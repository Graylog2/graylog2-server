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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public record CustomFieldMappings(@JsonProperty("mappings") Set<CustomFieldMapping> mappings) {

    public CustomFieldMappings() {
        this(Set.of());
    }

    public record CustomFieldMapping(@JsonProperty("field_name") String fieldName,
                                     @JsonProperty("type") String physicalType) {

        public Set<CustomFieldMapping> toSet() {
            return Set.of(this);
        }
    }

    public CustomFieldMappings modifiedWith(final Set<CustomFieldMapping> changedMappings) {
        final Set<CustomFieldMapping> modifiedMappings = new HashSet<>(mappings());
        modifiedMappings.removeIf(m -> changedMappings.stream().anyMatch(cm -> cm.fieldName().equals(m.fieldName())));
        modifiedMappings.addAll(changedMappings);
        return new CustomFieldMappings(modifiedMappings);
    }
}
