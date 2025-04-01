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
