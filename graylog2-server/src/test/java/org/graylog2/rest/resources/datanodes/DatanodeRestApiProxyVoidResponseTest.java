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
package org.graylog2.rest.resources.datanodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.http.POST;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests the null-body / 204 No Content handling in {@link DatanodeRestApiProxy#remoteInterface}.
 * Previously, a successful response with no body was treated as an error because the code asserted
 * {@code response.body() != null}. We need it to succeed (e.g. for Retrofit {@code Call<Void>} against
 * DataNode's {@code POST /management/start}).
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
@ExtendWith(MockitoExtension.class)
class DatanodeRestApiProxyVoidResponseTest {

    @Mock
    IndexerJwtAuthTokenProvider authTokenProvider;
    @Mock
    NodeService<DataNodeDto> nodeService;

    private final MockWebServer server = new MockWebServer();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DatanodeRestApiProxy proxy;

    private static final String NODE_ID = "test-node";
    private static final String HOSTNAME = "test-host";

    @BeforeEach
    void setUp() throws IOException {
        server.start();
        when(authTokenProvider.alwaysEnabled()).thenReturn(IndexerJwtAuthToken::disabled);

        final DataNodeDto node = DataNodeDto.Builder.builder()
                .setId(NODE_ID)
                .setHostname(HOSTNAME)
                .setClusterAddress("http://" + HOSTNAME + ":9300")
                .setTransportAddress("http://" + HOSTNAME + ":9200")
                .setRestApiAddress(server.url("/").toString())
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();
        when(nodeService.allActive()).thenReturn(Map.of(NODE_ID, node));

        proxy = new DatanodeRestApiProxy(
                authTokenProvider,
                nodeService,
                objectMapper,
                new DatanodeResolver(nodeService),
                httpClient,
                Duration.seconds(5));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    interface ManagementClient {
        @POST("management/stop")
        Call<Void> stop();
    }

    interface BodyReturningClient {
        @POST("payload")
        Call<Map<String, Object>> payload();
    }

    @Test
    void voidResponseWith200_succeeds_andMapsHostnameToNull() {
        server.enqueue(new MockResponse(200, Headers.of(), ""));

        final Map<String, Void> result = proxy.remoteInterface(HOSTNAME, ManagementClient.class, ManagementClient::stop);

        assertThat(result).containsOnlyKeys(HOSTNAME);
        assertThat(result.get(HOSTNAME)).isNull();
    }

    @Test
    void voidResponseWith204_succeeds_andMapsHostnameToNull() {
        server.enqueue(new MockResponse(204, Headers.of(), ""));

        final Map<String, Void> result = proxy.remoteInterface(HOSTNAME, ManagementClient.class, ManagementClient::stop);

        assertThat(result).containsOnlyKeys(HOSTNAME);
        assertThat(result.get(HOSTNAME)).isNull();
    }

    @Test
    void typedResponseWithBody_stillSucceeds_andReturnsBody() {
        server.enqueue(new MockResponse(200, Headers.of("Content-Type", "application/json"), "{\"hello\":\"world\"}"));

        final Map<String, Map<String, Object>> result = proxy.remoteInterface(HOSTNAME, BodyReturningClient.class, BodyReturningClient::payload);

        assertThat(result).containsOnlyKeys(HOSTNAME);
        assertThat(result.get(HOSTNAME)).containsEntry("hello", "world");
    }

    @Test
    void errorResponse_throws() {
        server.enqueue(new MockResponse(500, Headers.of(), "boom"));

        assertThatThrownBy(() -> proxy.remoteInterface(HOSTNAME, ManagementClient.class, ManagementClient::stop))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to trigger datanode request");
    }

    @Test
    void resultMap_isUnmodifiable() {
        server.enqueue(new MockResponse(200, Headers.of(), ""));
        final Map<String, Void> result = proxy.remoteInterface(HOSTNAME, ManagementClient.class, ManagementClient::stop);

        assertThatThrownBy(() -> result.put("x", null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
