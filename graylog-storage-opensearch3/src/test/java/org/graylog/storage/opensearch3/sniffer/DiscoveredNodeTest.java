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

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveredNodeTest {

    @Test
    void toURIProducesCorrectURI() {
        final var node = new DiscoveredNode("https", "my-host", 9200, Collections.emptyMap());
        assertThat(node.toURI()).isEqualTo(URI.create("https://my-host:9200"));
    }

    @Test
    void toHttpHostProducesCorrectHost() {
        final var node = new DiscoveredNode("http", "localhost", 9201, Collections.emptyMap());
        final HttpHost host = node.toHttpHost();
        assertThat(host.getSchemeName()).isEqualTo("http");
        assertThat(host.getHostName()).isEqualTo("localhost");
        assertThat(host.getPort()).isEqualTo(9201);
    }

    @Test
    void equalityBasedOnAllFields() {
        final var node1 = new DiscoveredNode("http", "host1", 9200, Map.of("rack", List.of("42")));
        final var node2 = new DiscoveredNode("http", "host1", 9200, Map.of("rack", List.of("42")));
        final var node3 = new DiscoveredNode("http", "host2", 9200, Collections.emptyMap());
        assertThat(node1).isEqualTo(node2);
        assertThat(node1).isNotEqualTo(node3);
    }

    @Test
    void toURIWithDefaultHttpPort() {
        final var node = new DiscoveredNode("http", "example.com", 80, Collections.emptyMap());
        assertThat(node.toURI()).isEqualTo(URI.create("http://example.com:80"));
    }

    @Test
    void toURIBracketsIPv6Address() {
        final var node = new DiscoveredNode("https", "::1", 9200, Collections.emptyMap());
        final URI uri = node.toURI();
        assertThat(uri.toString()).isEqualTo("https://[::1]:9200");
        assertThat(uri.getPort()).isEqualTo(9200);
    }

    @Test
    void toURIHandlesFullIPv6Address() {
        final var node = new DiscoveredNode("http", "2001:db8::1", 9200, Collections.emptyMap());
        final URI uri = node.toURI();
        assertThat(uri.toString()).isEqualTo("http://[2001:db8::1]:9200");
        assertThat(uri.getPort()).isEqualTo(9200);
    }
}
