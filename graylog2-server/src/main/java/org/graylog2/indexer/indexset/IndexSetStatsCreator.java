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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexSetStatsCreator {
    private final Indices indices;

    @Inject
    public IndexSetStatsCreator(final Indices indices) {
        this.indices = indices;
    }

    public IndexSetStats getForIndexSet(final IndexSet indexSet) {
        final Set<String> closedIndices = indices.getClosedIndices(indexSet);
        final List<JsonNode> primaries = indices.getIndexStats(indexSet).valueStream()
                .map(json -> json.get("primaries"))
                .toList();
        return toStats(primaries, closedIndices.size());
    }

    /**
     * Computes the stats of several index sets with one stats call and one closed-indices call.
     */
    public Map<String, IndexSetStats> getForIndexSets(final Collection<IndexSet> indexSets) {
        // An empty wildcard list would make the adapters query the whole cluster.
        if (indexSets.isEmpty()) {
            return Map.of();
        }
        final List<String> wildcards = indexSets.stream().map(IndexSet::getIndexWildcard).toList();
        final Set<String> closedIndices = indices.getClosedIndices(wildcards);
        final JsonNode stats = indices.getIndexStats(wildcards);

        final Map<String, IndexSetStats> result = new HashMap<>();
        for (final IndexSet indexSet : indexSets) {
            final List<JsonNode> primaries = stats.properties().stream()
                    .filter(entry -> indexSet.isManagedIndex(entry.getKey()))
                    .map(entry -> entry.getValue().path("primaries"))
                    .toList();
            final long closedCount = closedIndices.stream().filter(indexSet::isManagedIndex).count();
            result.put(indexSet.getConfig().id(), toStats(primaries, closedCount));
        }
        return result;
    }

    private static IndexSetStats toStats(final List<JsonNode> primaries, final long closedIndices) {
        final long documents = primaries.stream().mapToLong(json -> json.path("docs").path("count").asLong()).sum();
        final long size = primaries.stream().mapToLong(json -> json.path("store").path("size_in_bytes").asLong()).sum();
        return IndexSetStats.create(primaries.size() + closedIndices, documents, size);
    }
}
