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
package org.graylog2.configuration;

import org.assertj.core.api.Assertions;
import org.graylog2.bootstrap.preflight.PreflightConfig;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.cluster.Node;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class IndexerDiscoveryProviderTest {

    @Test
    void testAutomaticDiscovery() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                preflightConfig(PreflightConfigResult.FINISHED),
                nodes("http://localhost:9200", "http://other:9201")
        );

        Assertions.assertThat(provider.get())
                .hasSize(2)
                .extracting(URI::toString)
                .contains("http://localhost:9200", "http://other:9201");
    }


    @Test
    void testPreconfiguredIndexers() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                ImmutableList.of(URI.create("http://my-host:9200")),
                preflightConfig(null),
                nodes()
        );

        Assertions.assertThat(provider.get())
                .hasSize(1)
                .extracting(URI::toString)
                .contains("http://my-host:9200");
    }

    @Test
    void testSkippedConfigWithDefaultIndexer() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(),
                preflightConfig(PreflightConfigResult.SKIPPED),
                nodes()
        );

        Assertions.assertThat(provider.get())
                .hasSize(1)
                .extracting(URI::toString)
                .contains("http://127.0.0.1:9200");
    }

    @Test
    void testFailedAutodiscovery() {
        final IndexerDiscoveryProvider provider = new IndexerDiscoveryProvider(
                Collections.emptyList(), // no configured indexers
                preflightConfig(PreflightConfigResult.FINISHED), // preflight correctly finished
                nodes() // but still no nodes discovered
        );

        Assertions.assertThatThrownBy(provider::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("No Datanode available");
    }

    private Supplier<Collection<Node>> nodes(String... transportAddress) {
        return () -> Arrays.stream(transportAddress)
                .map(address -> {
                    final Node node = Mockito.mock(Node.class);
                    Mockito.when(node.getTransportAddress()).thenReturn(address);
                    return node;
                })
                .collect(Collectors.toList());
    }

    private Supplier<Optional<PreflightConfig>> preflightConfig(@Nullable PreflightConfigResult result) {
        return () -> Optional.ofNullable(result).map(r -> () -> r);
    }
}
