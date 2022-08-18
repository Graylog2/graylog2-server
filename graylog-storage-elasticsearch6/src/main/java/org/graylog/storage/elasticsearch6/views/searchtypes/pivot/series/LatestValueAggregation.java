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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot.series;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.core.search.aggregation.Aggregation;

import java.util.Optional;

public class LatestValueAggregation extends Aggregation {
    public LatestValueAggregation(String name, JsonNode jsonRoot) {
        super(name, jsonRoot);
    }

    public Optional<Object> getField(String field) {
        final JsonNode fieldValue = jsonRoot
                .path("hits")
                .path("hits")
                .path(0)
                .path("_source")
                .path(field);

        return fieldValue.isMissingNode() ? Optional.empty() : Optional.of(fieldValue);
    }
}
