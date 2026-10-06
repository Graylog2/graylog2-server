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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.storage.opensearch3.cluster.ClusterStateApi;
import org.graylog.storage.opensearch3.stats.ClusterStatsApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.storage.opensearch3.stats.StatsApi;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;

/**
 * Builds an {@link IndicesAdapterOS} backed by the admin-cert OpenSearch client. Used for the
 * {@code @AdminIndexer IndicesAdapter} binding, which routes its calls through
 * {@link AdminOpensearchClientProvider}.
 *
 * <p>T\he returned adapter holds
 * a stable client reference because {@link AdminOpensearchClientProvider} hot-swaps the
 * underlying transport rather than replacing the client.
 */
public class AdminIndicesAdapterProvider implements Provider<IndicesAdapter> {

    private final AdminOpensearchClientProvider adminClientProvider;
    private final StatsApi statsApi;
    private final ClusterStatsApi clusterStatsApi;
    private final ClusterStateApi clusterStateApi;
    private final IndexTemplateAdapter indexTemplateAdapter;
    private final IndexStatisticsBuilder indexStatisticsBuilder;
    private final ObjectMapper objectMapper;

    @Inject
    public AdminIndicesAdapterProvider(AdminOpensearchClientProvider adminClientProvider,
                                       StatsApi statsApi,
                                       ClusterStatsApi clusterStatsApi,
                                       ClusterStateApi clusterStateApi,
                                       IndexTemplateAdapter indexTemplateAdapter,
                                       IndexStatisticsBuilder indexStatisticsBuilder,
                                       ObjectMapper objectMapper) {
        this.adminClientProvider = adminClientProvider;
        this.statsApi = statsApi;
        this.clusterStatsApi = clusterStatsApi;
        this.clusterStateApi = clusterStateApi;
        this.indexTemplateAdapter = indexTemplateAdapter;
        this.indexStatisticsBuilder = indexStatisticsBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public IndicesAdapter get() {
        return new IndicesAdapterOS(
                adminClientProvider.getAdminClient(),
                statsApi,
                clusterStatsApi,
                clusterStateApi,
                indexTemplateAdapter,
                indexStatisticsBuilder,
                objectMapper);
    }
}
