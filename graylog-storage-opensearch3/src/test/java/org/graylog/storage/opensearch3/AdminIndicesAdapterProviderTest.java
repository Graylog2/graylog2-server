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
import org.graylog.storage.opensearch3.cluster.ClusterStateApi;
import org.graylog.storage.opensearch3.stats.ClusterStatsApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.storage.opensearch3.stats.StatsApi;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminIndicesAdapterProviderTest {

    @Mock
    private AdminOpensearchClientProvider adminClientProvider;
    @Mock
    private StatsApi statsApi;
    @Mock
    private ClusterStatsApi clusterStatsApi;
    @Mock
    private ClusterStateApi clusterStateApi;
    @Mock
    private IndexTemplateAdapter indexTemplateAdapter;
    @Mock
    private IndexStatisticsBuilder indexStatisticsBuilder;

    @Test
    void buildsIndicesAdapterUsingAdminClient() {
        final OfficialOpensearchClient adminClient = ServerlessOpenSearchClient.builder().build();
        when(adminClientProvider.getAdminClient()).thenReturn(adminClient);
        final ObjectMapper objectMapper = new ObjectMapper();

        final AdminIndicesAdapterProvider provider = new AdminIndicesAdapterProvider(
                adminClientProvider, statsApi, clusterStatsApi, clusterStateApi,
                indexTemplateAdapter, indexStatisticsBuilder, objectMapper);

        final IndicesAdapter adapter = provider.get();

        assertThat(adapter).isInstanceOf(IndicesAdapterOS.class);
        verify(adminClientProvider).getAdminClient();
    }
}
