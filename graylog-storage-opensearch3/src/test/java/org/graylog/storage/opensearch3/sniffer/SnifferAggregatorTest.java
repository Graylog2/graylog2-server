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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class SnifferAggregatorTest {

    @Test
    void sniffOneSniffer() {
        final var aggregator = new SnifferAggregator(
                List.of(() -> List.of(node("http", "localhost", 9200))),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .allSatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://localhost:9200"));
    }

    @Test
    void sniffMoreSniffersDifferentNodes() {
        final var aggregator = new SnifferAggregator(
                List.of(
                        () -> List.of(node("http", "localhost", 9200)),
                        () -> List.of(node("http", "second-node", 9200))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(2)
                .anySatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://localhost:9200"))
                .anySatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://second-node:9200"));
    }

    @Test
    void sniffMoreSniffersSameNode() {
        final var aggregator = new SnifferAggregator(
                List.of(
                        () -> List.of(node("http", "localhost", 9200)),
                        () -> List.of(node("http", "localhost", 9200))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .anySatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://localhost:9200"));
    }

    @Test
    void sniffFilters() {
        final var aggregator = new SnifferAggregator(
                List.of(
                        () -> List.of(node("http", "localhost", 9200)),
                        () -> List.of(node("http", "second-node", 9200))
                ),
                List.of(createFilter()));

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .anySatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://second-node:9200"));
    }

    @Test
    void snifferFailureDoesNotBreakOthers() {
        final NodesSniffer failingSniffer = new NodesSniffer() {
            @Override
            public boolean enabled() { return true; }
            @Override
            public List<DiscoveredNode> sniff() throws IOException { throw new IOException("connection refused"); }
        };

        final var aggregator = new SnifferAggregator(
                List.of(
                        failingSniffer,
                        () -> List.of(node("http", "localhost", 9200))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .anySatisfy(n -> Assertions.assertThat(n.toURI().toString()).isEqualTo("http://localhost:9200"));
    }

    private static SnifferFilter createFilter() {
        return new SnifferFilter() {
            @Override
            public boolean enabled() { return true; }
            @Override
            public List<DiscoveredNode> filterNodes(List<DiscoveredNode> nodes) {
                return nodes.stream()
                        .filter(n -> n.host().contains("second"))
                        .collect(Collectors.toList());
            }
        };
    }

    private DiscoveredNode node(String scheme, String host, int port) {
        return new DiscoveredNode(scheme, host, port, Collections.emptyMap());
    }
}
