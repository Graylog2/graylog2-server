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
package org.graylog.storage.opensearch3.sniffer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.sniffer.impl.OpensearchClusterSniffer;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.generic.Request;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpensearchClusterSnifferTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OfficialOpensearchClient client;
    private ElasticsearchClientConfiguration configuration;

    @BeforeEach
    void setUp() {
        client = mock(OfficialOpensearchClient.class);
        configuration = mock(ElasticsearchClientConfiguration.class);
        when(configuration.defaultSchemeForDiscoveredNodes()).thenReturn("http");
    }

    @Test
    void parsesNodesFromResponse() throws Exception {
        final String json = """
                {
                  "nodes": {
                    "node1": {
                      "http": { "publish_address": "10.0.0.1:9200" },
                      "attributes": { "rack": "42" }
                    },
                    "node2": {
                      "http": { "publish_address": "10.0.0.2:9200" },
                      "attributes": {}
                    }
                  }
                }
                """;
        final JsonNode jsonNode = objectMapper.readTree(json);
        when(client.performRequest(any(Request.class), anyString())).thenReturn(jsonNode);
        when(configuration.discoveryEnabled()).thenReturn(true);

        final var sniffer = new OpensearchClusterSniffer(client, configuration);
        final List<DiscoveredNode> nodes = sniffer.sniff();

        assertThat(nodes).hasSize(2);
        assertThat(nodes).anySatisfy(n -> {
            assertThat(n.host()).isEqualTo("10.0.0.1");
            assertThat(n.port()).isEqualTo(9200);
            assertThat(n.scheme()).isEqualTo("http");
            assertThat(n.attributes()).containsEntry("rack", List.of("42"));
        });
        assertThat(nodes).anySatisfy(n -> {
            assertThat(n.host()).isEqualTo("10.0.0.2");
            assertThat(n.port()).isEqualTo(9200);
            assertThat(n.attributes()).isEmpty();
        });
    }

    @Test
    void handlesPublishAddressWithHostnameSlashIp() throws Exception {
        final String json = """
                {
                  "nodes": {
                    "node1": {
                      "http": { "publish_address": "my-host/10.0.0.1:9200" },
                      "attributes": {}
                    }
                  }
                }
                """;
        final JsonNode jsonNode = objectMapper.readTree(json);
        when(client.performRequest(any(Request.class), anyString())).thenReturn(jsonNode);
        when(configuration.discoveryEnabled()).thenReturn(true);

        final var sniffer = new OpensearchClusterSniffer(client, configuration);
        final List<DiscoveredNode> nodes = sniffer.sniff();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().host()).isEqualTo("10.0.0.1");
        assertThat(nodes.getFirst().port()).isEqualTo(9200);
    }

    @Test
    void parsesIPv6PublishAddress() throws Exception {
        final String json = """
                {
                  "nodes": {
                    "node1": {
                      "http": { "publish_address": "[::1]:9200" },
                      "attributes": {}
                    }
                  }
                }
                """;
        final JsonNode jsonNode = objectMapper.readTree(json);
        when(client.performRequest(any(Request.class), anyString())).thenReturn(jsonNode);
        when(configuration.discoveryEnabled()).thenReturn(true);

        final var sniffer = new OpensearchClusterSniffer(client, configuration);
        final List<DiscoveredNode> nodes = sniffer.sniff();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().host()).isEqualTo("::1");
        assertThat(nodes.getFirst().port()).isEqualTo(9200);
    }

    @Test
    void parsesIPv6PublishAddressWithHostnamePrefix() throws Exception {
        final String json = """
                {
                  "nodes": {
                    "node1": {
                      "http": { "publish_address": "my-host/[2001:db8::1]:9200" },
                      "attributes": {}
                    }
                  }
                }
                """;
        final JsonNode jsonNode = objectMapper.readTree(json);
        when(client.performRequest(any(Request.class), anyString())).thenReturn(jsonNode);
        when(configuration.discoveryEnabled()).thenReturn(true);

        final var sniffer = new OpensearchClusterSniffer(client, configuration);
        final List<DiscoveredNode> nodes = sniffer.sniff();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().host()).isEqualTo("2001:db8::1");
        assertThat(nodes.getFirst().port()).isEqualTo(9200);
    }

    @Test
    void enabledWhenDiscoveryEnabledOrNodeActivityLogger() {
        when(configuration.discoveryEnabled()).thenReturn(false);
        when(configuration.isNodeActivityLogger()).thenReturn(false);
        final var sniffer = new OpensearchClusterSniffer(client, configuration);
        assertThat(sniffer.enabled()).isFalse();

        when(configuration.discoveryEnabled()).thenReturn(true);
        final var sniffer2 = new OpensearchClusterSniffer(client, configuration);
        assertThat(sniffer2.enabled()).isTrue();

        when(configuration.discoveryEnabled()).thenReturn(false);
        when(configuration.isNodeActivityLogger()).thenReturn(true);
        final var sniffer3 = new OpensearchClusterSniffer(client, configuration);
        assertThat(sniffer3.enabled()).isTrue();
    }
}
