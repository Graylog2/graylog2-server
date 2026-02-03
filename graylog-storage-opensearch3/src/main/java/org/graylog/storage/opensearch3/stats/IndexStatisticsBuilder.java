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
package org.graylog.storage.opensearch3.stats;


import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.rest.models.system.indexer.responses.IndexStats.TimeAndTotalStats;
import org.opensearch.client.opensearch._types.DocStats;
import org.opensearch.client.opensearch._types.FlushStats;
import org.opensearch.client.opensearch._types.GetStats;
import org.opensearch.client.opensearch._types.IndexingStats;
import org.opensearch.client.opensearch._types.MergesStats;
import org.opensearch.client.opensearch._types.RefreshStats;
import org.opensearch.client.opensearch._types.SearchStats;
import org.opensearch.client.opensearch._types.SegmentsStats;
import org.opensearch.client.opensearch._types.StoreStats;
import org.opensearch.client.opensearch.indices.stats.IndexShardStats;
import org.opensearch.client.opensearch.indices.stats.IndexStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.stats.ShardRouting;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class IndexStatisticsBuilder {

    public IndexStatistics build(final String index, final IndicesStats indicesStats) {
        final IndexStats primaries = indicesStats.primaries();
        final IndexStats total = indicesStats.total();
        final Map<String, List<IndexShardStats>> shards = indicesStats.shards();

        return IndexStatistics.create(index, buildIndexStats(primaries), buildIndexStats(total), buildShardRoutings(shards));
    }

    private org.graylog2.rest.models.system.indexer.responses.IndexStats buildIndexStats(final IndexStats stats) {

        final Optional<DocStats> docs = Optional.ofNullable(stats.docs());
        final long docsCount = docs.map(DocStats::count).orElse(0L);
        final long docsDeleted = docs.map(DocStats::deleted).orElse(0L);

        final SearchStats search = stats.search();
        return org.graylog2.rest.models.system.indexer.responses.IndexStats.create(
                createTimeAndTotalStats(stats.flush(), FlushStats::total, FlushStats::totalTimeInMillis),
                createTimeAndTotalStats(stats.get(), GetStats::total, GetStats::timeInMillis),
                createTimeAndTotalStats(stats.indexing(), IndexingStats::indexTotal, IndexingStats::indexTimeInMillis),
                createTimeAndTotalStats(stats.merges(), MergesStats::total, MergesStats::totalTimeInMillis),
                createTimeAndTotalStats(stats.refresh(), RefreshStats::total, RefreshStats::totalTimeInMillis),
                createTimeAndTotalStats(search, SearchStats::queryTotal, SearchStats::queryTimeInMillis),
                createTimeAndTotalStats(search, SearchStats::fetchTotal, SearchStats::fetchTimeInMillis),
                Optional.ofNullable(search).map(SearchStats::openContexts).orElse(0L),
                Optional.ofNullable(stats.store()).map(StoreStats::sizeInBytes).orElse(0L),
                Optional.ofNullable(stats.segments()).map(SegmentsStats::count).orElse(0),
                org.graylog2.rest.models.system.indexer.responses.IndexStats.DocsStats.create(docsCount, docsDeleted)
        );


    }

    private <T> TimeAndTotalStats createTimeAndTotalStats(final T stats,
                                                          final Function<T, Long> totalMapper,
                                                          final Function<T, Long> totalTimeInMillisMapper
    ) {
        return TimeAndTotalStats.create(
                stats != null ? totalMapper.apply(stats) : 0L,
                stats != null ? totalTimeInMillisMapper.apply(stats) / 1000L : 0L
        );
    }

    private static List<org.graylog2.rest.models.system.indexer.responses.ShardRouting> buildShardRoutings(Map<String, List<IndexShardStats>> shardRoutings) {
        return shardRoutings.entrySet().stream()
                .map(entry -> {
                    final int shardId = Integer.parseInt(entry.getKey());
                    final List<IndexShardStats> shards = entry.getValue();

                    return shards.stream().map(shard -> {
                        final Optional<ShardRouting> routing = Optional.ofNullable(shard.routing());
                        final String state = routing.map(ShardRouting::state)
                                .map(st -> st.jsonValue().toLowerCase(Locale.ENGLISH))
                                .orElse("unknown");

                        // Taken from org.elasticsearch.cluster.routing.ShardRouting
                        final boolean active = "started".equals(state) || "relocating".equals(state);

                        final boolean primary = routing.map(ShardRouting::primary).orElse(false);
                        final String nodeId = routing.map(ShardRouting::node).orElse("Unknown");

                        // Node name and hostname should be filled when necessary (requiring an additional round trip to Elasticsearch)
                        final String nodeName = null;
                        final String nodeHostname = null;

                        final String relocatingNode = routing.map(ShardRouting::relocatingNode).orElse(null);

                        final org.graylog2.rest.models.system.indexer.responses.ShardRouting shardRouting =
                                org.graylog2.rest.models.system.indexer.responses.ShardRouting.create(
                                        shardId,
                                        state,
                                        active,
                                        primary,
                                        nodeId,
                                        nodeName,
                                        nodeHostname,
                                        relocatingNode
                                );
                        return shardRouting;
                    }).toList();
                }).flatMap(List::stream)
                .sorted(Comparator.comparing(org.graylog2.rest.models.system.indexer.responses.ShardRouting::id).thenComparing(org.graylog2.rest.models.system.indexer.responses.ShardRouting::nodeId))
                .toList();

    }
}
