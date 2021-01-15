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
package org.graylog2.indexer.indices.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog2.rest.models.system.indexer.responses.IndexStats;
import org.graylog2.rest.models.system.indexer.responses.ShardRouting;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@AutoValue
public abstract class IndexStatistics {
    public abstract String index();

    public abstract IndexStats primaryShards();

    public abstract IndexStats allShards();

    public abstract List<ShardRouting> routing();

    public static IndexStatistics create(String index,
                                         IndexStats primaryShards,
                                         IndexStats allShards,
                                         List<ShardRouting> routing) {
        return new AutoValue_IndexStatistics(index, primaryShards, allShards, routing);
    }

    public static IndexStatistics create(String index, JsonNode indexStats) {
        final JsonNode primaries = indexStats.path("primaries");
        final JsonNode total = indexStats.path("total");
        final JsonNode shards = indexStats.path("shards");


        return create(index, buildIndexStats(primaries), buildIndexStats(total), buildShardRoutings(shards));
    }

    private static IndexStats buildIndexStats(final JsonNode stats) {
        final JsonNode flush = stats.path("flush");
        final long flushTotal = flush.path("total").asLong();
        final long flushTotalTimeSeconds = flush.path("total_time_in_millis").asLong() / 1000L;

        final JsonNode get = stats.path("get");
        final long getTotal = get.path("total").asLong();
        final long getTotalTimeSeconds = get.path("total_time_in_millis").asLong() / 1000L;

        final JsonNode indexing = stats.path("indexing");
        final long indexingTotal = indexing.path("index_total").asLong();
        final long indexingTotalTimeSeconds = indexing.path("index_time_in_millis").asLong() / 1000L;

        final JsonNode merge = stats.path("merges");
        final long mergeTotal = merge.path("total").asLong();
        final long mergeTotalTimeSeconds = merge.path("total_time_in_millis").asLong() / 1000L;

        final JsonNode refresh = stats.path("refresh");
        final long refreshTotal = refresh.path("total").asLong();
        final long refreshTotalTimeSeconds = refresh.path("total_time_in_millis").asLong() / 1000L;

        final JsonNode search = stats.path("search");
        final long searchQueryTotal = search.path("query_total").asLong();
        final long searchQueryTotalTimeSeconds = search.path("query_time_in_millis").asLong() / 1000L;
        final long searchFetchTotal = search.path("fetch_total").asLong();
        final long searchFetchTotalTimeSeconds = search.path("fetch_time_in_millis").asLong() / 1000L;
        final long searchOpenContexts = search.path("open_contexts").asLong();

        final long storeSizeInBytes = stats.path("store").path("size_in_bytes").asLong();
        final long segmentsCount = stats.path("segments").path("count").asLong();

        final JsonNode docs = stats.path("docs");
        final long docsCount = docs.path("count").asLong();
        final long docsDeleted = docs.path("deleted").asLong();

        return IndexStats.create(
                IndexStats.TimeAndTotalStats.create(flushTotal, flushTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(getTotal, getTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(indexingTotal, indexingTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(mergeTotal, mergeTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(refreshTotal, refreshTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(searchQueryTotal, searchQueryTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(searchFetchTotal, searchFetchTotalTimeSeconds),
                searchOpenContexts,
                storeSizeInBytes,
                segmentsCount,
                IndexStats.DocsStats.create(docsCount, docsDeleted)
        );
    }

    private static List<ShardRouting> buildShardRoutings(JsonNode shardRoutings) {
        final ImmutableList.Builder<ShardRouting> shardRoutingsBuilder = ImmutableList.builder();
        final Iterator<Map.Entry<String, JsonNode>> it = shardRoutings.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final int shardId = Integer.parseInt(entry.getKey());
            final JsonNode shards = entry.getValue();

            for (JsonNode jsonElement : shards) {
                final JsonNode routing = jsonElement.path("routing");
                final String state = routing.path("state").asText("unknown").toLowerCase(Locale.ENGLISH);

                // Taken from org.elasticsearch.cluster.routing.ShardRouting
                final boolean active = "started".equals(state) || "relocating".equals(state);

                final boolean primary = routing.path("primary").asBoolean(false);
                final String nodeId = routing.path("node").asText("Unknown");

                // Node name and hostname should be filled when necessary (requiring an additional round trip to Elasticsearch)
                final String nodeName = null;
                final String nodeHostname = null;

                final String relocatingNode = routing.path("relocating_node").asText(null);

                final ShardRouting shardRouting = ShardRouting.create(shardId, state, active, primary, nodeId, nodeName, nodeHostname, relocatingNode);
                shardRoutingsBuilder.add(shardRouting);
            }
        }
        return shardRoutingsBuilder.build();
    }
}
