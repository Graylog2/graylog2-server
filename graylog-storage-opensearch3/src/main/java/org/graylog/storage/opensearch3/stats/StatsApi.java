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

import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch._types.Level;
import org.opensearch.client.opensearch._types.StoreStats;
import org.opensearch.client.opensearch.indices.IndicesStatsRequest;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.stats.IndexStatsBase;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStatsMetric;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StatsApi {

    private final OfficialOpensearchClient client;

    @Inject
    public StatsApi(final OfficialOpensearchClient client) {
        this.client = client;
    }

    public IndicesStats indexStatsWithShardLevel(final String index) {
        return indicesStatsWithShardLevel(Collections.singleton(index)).get(index);
    }

    public Map<String, IndicesStats> indicesStatsWithShardLevel(Collection<String> indices) {
        final IndicesStatsResponse stats = stats(indices, List.of(), true);
        return stats.indices();
    }

    public Map<String, IndicesStats> indicesStatsWithDocsAndStore(Collection<String> indices) {
        final IndicesStatsResponse stats = stats(indices, List.of(IndicesStatsMetric.Store, IndicesStatsMetric.Docs), false);
        return stats.indices();
    }

    public Optional<Long> storeSizes(String index) {
        final IndicesStatsResponse stats = stats(Collections.singleton(index), List.of(IndicesStatsMetric.Store), false);
        return Optional.ofNullable(
                        stats.indices()
                                .get(index)
                ).map(IndicesStats::primaries)
                .map(IndexStatsBase::store)
                .map(StoreStats::sizeInBytes);
    }

    private IndicesStatsResponse stats(Collection<String> indices,
                                       List<IndicesStatsMetric> metrics,
                                       boolean withShardLevel) {
        IndicesStatsRequest.Builder builder = IndicesStatsRequest.builder()
                .index(indices.stream().toList())
                .metric(metrics);
        if (withShardLevel) {
//            request.addParameter("ignore_unavailable", "true"); //TODO "ignore_unavailable" has no equivalent?
            builder = builder.level(Level.Shards);
        }
        final IndicesStatsRequest indicesStatsRequest = builder.build();
        final OpenSearchIndicesClient indicesClient = client.sync().indices();
        return client.execute(() -> indicesClient.stats(indicesStatsRequest), "Unable to retrieve stats for indices");
    }


}
