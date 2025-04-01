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
package org.graylog.storage.opensearch2.sniffer;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class SnifferAggregatorTest {

    @Test
    void sniffOneSniffer() throws IOException {
        final SnifferAggregator aggregator = new SnifferAggregator(
                List.of(
                        () -> Collections.singletonList(node("http://localhost:9200"))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .allSatisfy(node -> Assertions.assertThat(node.getHost().toURI()).isEqualTo("http://localhost:9200"));
    }

    @Test
    void sniffMoreSniffersDifferentNodes() throws IOException {
        final SnifferAggregator aggregator = new SnifferAggregator(
                List.of(
                        () -> Collections.singletonList(node("http://localhost:9200")),
                        () -> Collections.singletonList(node("http://second-node:9200"))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(2)
                .anySatisfy(node -> Assertions.assertThat(node.getHost().toURI()).isEqualTo("http://localhost:9200"))
                .anySatisfy(node -> Assertions.assertThat(node.getHost().toURI()).isEqualTo("http://second-node:9200"));
    }

    @Test
    void sniffMoreSniffersSameNode() throws IOException {
        final SnifferAggregator aggregator = new SnifferAggregator(
                List.of(
                        () -> Collections.singletonList(node("http://localhost:9200")),
                        () -> Collections.singletonList(node("http://localhost:9200"))
                ),
                Collections.emptyList());

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .anySatisfy(node -> Assertions.assertThat(node.getHost().toURI()).isEqualTo("http://localhost:9200"));
    }

    @Test
    void sniffFilters() throws IOException {
        final SnifferAggregator aggregator = new SnifferAggregator(
                List.of(
                        () -> Collections.singletonList(node("http://localhost:9200")),
                        () -> Collections.singletonList(node("http://second-node:9200"))
                ),
                List.of(
                        createFilter(n -> n.getHost().toURI().contains("second"))
                ));

        Assertions.assertThat(aggregator.sniff())
                .hasSize(1)
                .anySatisfy(node -> Assertions.assertThat(node.getHost().toURI()).isEqualTo("http://second-node:9200"));
    }

    @Nonnull
    private static SnifferFilter createFilter(Predicate<Node> predicate) {
        return new SnifferFilter() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public List<Node> filterNodes(List<Node> nodes) {
                return nodes.stream().filter(predicate).collect(Collectors.toList());
            }
        };
    }


    private Node node(String url) {
        return new Node(HttpHost.create(url));
    }
}
