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
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.opensearch.client.opensearch.cluster.ClusterStatsRequest;
import org.opensearch.client.opensearch.cluster.ClusterStatsResponse;
import org.opensearch.client.opensearch.cluster.stats.IndexMetric;
import org.opensearch.client.opensearch.cluster.stats.Metric;

import java.util.List;

public class ClusterStatsApi {
    private final OfficialOpensearchClient officialOpensearchClient;

    @Inject
    public ClusterStatsApi(OfficialOpensearchClient officialOpensearchClient) {
        this.officialOpensearchClient = officialOpensearchClient;
    }

    public IndexSetStats clusterStats() {
        final ClusterStatsRequest request = ClusterStatsRequest.builder()
                .nodeId("_all")
                .metric(List.of(Metric.Indices))
                .indexMetric(List.of(IndexMetric.Store, IndexMetric.Docs))
                .build();

        final ClusterStatsResponse stats = officialOpensearchClient.sync(c -> c.cluster().stats(request), "Couldn't read OpensSearch cluster stats");
        return IndexSetStats.create(stats.indices().count(), stats.indices().docs().count(), stats.indices().store().sizeInBytes());
    }
}
