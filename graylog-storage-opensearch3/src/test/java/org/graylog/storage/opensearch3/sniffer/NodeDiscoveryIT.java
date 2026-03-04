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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.graylog.storage.opensearch3.sniffer.impl.OpensearchClusterSniffer;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDiscoveryIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Test
    void opensearchClusterSnifferDiscoversContainerNode() throws IOException {
        final ElasticsearchClientConfiguration configuration = buildConfig(Map.of(
                "elasticsearch_connect_timeout", "60s",
                "elasticsearch_socket_timeout", "60s",
                "elasticsearch_max_total_connections", "1",
                "elasticsearch_max_total_connections_per_route", "1",
                "elasticsearch_use_expect_continue", "false"
        ));

        final OpensearchClusterSniffer sniffer = new OpensearchClusterSniffer(
                openSearchInstance.getOfficialOpensearchClient(),
                configuration
        );

        final List<DiscoveredNode> nodes = sniffer.sniff();

        assertThat(nodes).isNotEmpty();
        assertThat(nodes).allSatisfy(node -> {
            assertThat(node.scheme()).isEqualTo("http");
            assertThat(node.host()).isNotBlank();
            assertThat(node.port()).isGreaterThan(0);
        });
    }

    @Test
    void snifferAggregatorCombinesAndFilters() throws IOException {
        final ElasticsearchClientConfiguration configuration = buildConfig(Map.of(
                "elasticsearch_connect_timeout", "60s",
                "elasticsearch_socket_timeout", "60s",
                "elasticsearch_max_total_connections", "1",
                "elasticsearch_max_total_connections_per_route", "1",
                "elasticsearch_use_expect_continue", "false"
        ));

        final OpensearchClusterSniffer sniffer = new OpensearchClusterSniffer(
                openSearchInstance.getOfficialOpensearchClient(),
                configuration
        );

        final SnifferAggregator aggregator = new SnifferAggregator(
                List.of(sniffer),
                Collections.emptyList()
        );

        final List<DiscoveredNode> nodes = aggregator.sniff();

        assertThat(nodes).isNotEmpty();
        // Verify nodes are deduplicated (single container = single node)
        assertThat(nodes).hasSize(1);
    }

    private static ElasticsearchClientConfiguration buildConfig(Map<String, String> properties) {
        final ElasticsearchClientConfiguration config = new ElasticsearchClientConfiguration();
        try {
            new JadConfig(new InMemoryRepository(properties), config).process();
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
