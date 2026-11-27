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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests of {@link DatanodeRestApiProxy#request} against real HTTP servers standing in for data nodes:
 * verifies which node a request is sent to and what path and query the data node actually receives.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatanodeRestApiProxyTest {

    @Mock
    IndexerJwtAuthTokenProvider authTokenProvider;
    @Mock
    NodeService<DataNodeDto> nodeService;

    private final MockWebServer node1 = new MockWebServer();
    private final MockWebServer node2 = new MockWebServer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DatanodeRestApiProxy proxy;

    @BeforeEach
    void setUp() throws IOException {
        node1.start();
        node2.start();
        when(authTokenProvider.alwaysEnabled()).thenReturn(IndexerJwtAuthToken::disabled);

        final Map<String, DataNodeDto> nodes = new LinkedHashMap<>();
        nodes.put("id1", node("id1", "datanode1", node1));
        nodes.put("id2", node("id2", "datanode2", node2));
        when(nodeService.allActive()).thenReturn(nodes);

        proxy = new DatanodeRestApiProxy(authTokenProvider, nodeService, objectMapper, new DatanodeResolver(nodeService),
                new OkHttpClient(), Duration.seconds(5));
    }

    @AfterEach
    void tearDown() throws IOException {
        node1.close();
        node2.close();
    }

    private static DataNodeDto node(String id, String hostname, MockWebServer server) {
        return DataNodeDto.Builder.builder()
                .setId(id)
                .setHostname(hostname)
                .setTransportAddress("http://" + hostname + ":9200")
                // registered with a trailing slash, like real data nodes do
                .setRestApiAddress(server.url("/").toString())
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();
    }

    private static ProxyRequestAdapter.ProxyRequest request(String method, String hostname, String path, MultivaluedMap<String, String> query) {
        return new ProxyRequestAdapter.ProxyRequest(method, path, InputStream.nullInputStream(), hostname, query);
    }

    private static ProxyRequestAdapter.ProxyRequest get(String hostname, String path) {
        return request("GET", hostname, path, new MultivaluedHashMap<>());
    }

    private static MockResponse json(String body) {
        return new MockResponse(200, Headers.of("Content-Type", "application/json"), body);
    }

    private static void assertSentTo(RecordedRequest recorded, MockWebServer server) {
        assertThat(recorded.getUrl().host()).isEqualTo(server.getHostName());
        assertThat(recorded.getUrl().port()).isEqualTo(server.getPort());
    }

    @Test
    void forwardsPathAndQueryToSelectedNode() throws Exception {
        node2.enqueue(json("{}"));
        final MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("format", "json");
        query.add("h", "index,health");

        final ProxyRequestAdapter.ProxyResponse response = proxy.request(request("GET", "datanode2", "_cat/indices", query));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("application/json");
        final RecordedRequest recorded = node2.takeRequest();
        assertSentTo(recorded, node2);
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getTarget()).isEqualTo("/_cat/indices?format=json&h=index%2Chealth");
        assertThat(node1.getRequestCount()).isZero();
    }

    @Test
    void forwardsBodyForPost() throws Exception {
        node1.enqueue(json("{}"));
        final var request = new ProxyRequestAdapter.ProxyRequest("POST", "metrics/multiple",
                new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8)), "datanode1", new MultivaluedHashMap<>());

        proxy.request(request);

        final RecordedRequest recorded = node1.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getTarget()).isEqualTo("/metrics/multiple");
        assertThat(recorded.getBody()).isNotNull();
        assertThat(recorded.getBody().utf8()).isEqualTo("{\"a\":1}");
    }

    @Test
    void injectionAttemptsStayEncodedInPathAndQuery() throws Exception {
        node1.enqueue(json("{}"));
        final MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("a&b=c", "1#@evil.example.com/x");

        proxy.request(request("GET", "datanode1", "_cat?x=1#@evil.example.com/y", query));

        final RecordedRequest recorded = node1.takeRequest();
        assertSentTo(recorded, node1);
        final HttpUrl url = recorded.getUrl();
        assertThat(url.pathSegments()).containsExactly("_cat?x=1#@evil.example.com", "y");
        assertThat(url.queryParameterNames()).containsExactly("a&b=c");
        assertThat(url.queryParameter("a&b=c")).isEqualTo("1#@evil.example.com/x");
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.example.com", "http://evil.example.com", "datanode1.evil.example.com", "datanode1@evil.example.com", "127.0.0.1"})
    void unknownHostnameIsRejectedWithoutSendingRequest(String hostname) {
        assertThatThrownBy(() -> proxy.request(get(hostname, "_cat/indices")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No datanode found");
        assertThat(node1.getRequestCount()).isZero();
        assertThat(node2.getRequestCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secrets", "_cat/../../secrets"})
    void traversalIsRejectedWithoutSendingRequest(String path) {
        assertThatThrownBy(() -> proxy.request(get("datanode1", path)))
                .isInstanceOf(RequestNotAllowedException.class);
        assertThat(node1.getRequestCount()).isZero();
    }

    @Test
    void anyKeywordSendsToARegisteredNode() throws Exception {
        node1.enqueue(json("{}"));
        node2.enqueue(json("{}"));

        proxy.request(get("any", "_cluster/health"));

        assertThat(node1.getRequestCount() + node2.getRequestCount()).isEqualTo(1);
    }

    @Test
    void allKeywordFansOutToEveryRegisteredNode() throws Exception {
        node1.enqueue(json("{\"node\":1}"));
        node2.enqueue(json("{\"node\":2}"));

        final ProxyRequestAdapter.ProxyResponse response = proxy.request(get(DatanodeResolver.ALL_NODES_KEYWORD, "_cluster/health"));

        final JsonNode result = objectMapper.readTree(response.response());
        assertThat(result.get("datanode1").get("node").asInt()).isEqualTo(1);
        assertThat(result.get("datanode2").get("node").asInt()).isEqualTo(2);
        assertThat(node1.takeRequest().getTarget()).isEqualTo("/_cluster/health");
        assertThat(node2.takeRequest().getTarget()).isEqualTo("/_cluster/health");
    }
}
