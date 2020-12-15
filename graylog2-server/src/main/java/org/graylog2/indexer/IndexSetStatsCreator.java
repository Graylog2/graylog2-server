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
package org.graylog2.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IndexSetStatsCreator {
    private final Indices indices;

    @Inject
    public IndexSetStatsCreator(final Indices indices) {
        this.indices = indices;
    }

    public IndexSetStats getForIndexSet(final IndexSet indexSet) {
        final Set<String> closedIndices = indices.getClosedIndices(indexSet);
        final List<JsonNode> primaries = StreamSupport.stream(indices.getIndexStats(indexSet).spliterator(), false)
                .map(json -> json.get("primaries"))
                .collect(Collectors.toList());
        final long documents = primaries.stream()
                .map(json -> json.path("docs").path("count").asLong())
                .reduce(0L, Long::sum);
        final long size = primaries.stream()
                .map(json -> json.path("store").path("size_in_bytes").asLong())
                .reduce(0L, Long::sum);

        return IndexSetStats.create(primaries.size() + closedIndices.size(), documents, size);
    }
}
