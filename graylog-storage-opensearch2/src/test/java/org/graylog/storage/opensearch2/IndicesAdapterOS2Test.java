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

package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.apache.http.HttpVersion;
import org.graylog.shaded.opensearch2.org.apache.http.message.BasicRequestLine;
import org.graylog.shaded.opensearch2.org.apache.http.message.BasicStatusLine;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.ResponseException;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch2.cat.CatApi;
import org.graylog.storage.opensearch2.cluster.ClusterStateApi;
import org.graylog.storage.opensearch2.stats.ClusterStatsApi;
import org.graylog.storage.opensearch2.stats.StatsApi;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicesAdapterOS2Test {
    private static final List<String> INDICES = List.of("graylog_*");

    @Mock
    private RestHighLevelClient highLevelClient;
    @Mock
    private RestClient lowLevelClient;
    @Mock
    private CatApi catApi;

    private IndicesAdapterOS2 adapter;

    @BeforeEach
    void setUp() {
        when(highLevelClient.getLowLevelClient()).thenReturn(lowLevelClient);
        final OpenSearchClient client = new OpenSearchClient(highLevelClient, new ObjectMapper());
        adapter = new IndicesAdapterOS2(client, mock(StatsApi.class), mock(ClusterStatsApi.class), catApi,
                mock(ClusterStateApi.class), mock(IndexTemplateAdapter.class));
    }

    @Test
    void closedIndicesFallsBackToCatApiWhenResolveIsForbidden() throws IOException {
        final ResponseException forbidden = responseException(403);
        when(lowLevelClient.performRequest(any())).thenThrow(forbidden);
        when(catApi.indices(eq(INDICES), eq(Set.of("close")), anyString())).thenReturn(Set.of("graylog_0"));

        assertThat(adapter.closedIndices(INDICES)).containsExactly("graylog_0");
    }

    @Test
    void closedIndicesPropagatesOtherResolveFailures() throws IOException {
        final ResponseException failure = responseException(500);
        when(lowLevelClient.performRequest(any())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.closedIndices(INDICES))
                .isInstanceOf(OpenSearchException.class)
                .hasCause(failure);
        verifyNoInteractions(catApi);
    }

    private static ResponseException responseException(int status) throws IOException {
        final Response response = mock(Response.class);
        when(response.getRequestLine()).thenReturn(new BasicRequestLine("GET", "/_resolve/index/graylog_*", HttpVersion.HTTP_1_1));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, status, null));
        return new ResponseException(response);
    }
}
