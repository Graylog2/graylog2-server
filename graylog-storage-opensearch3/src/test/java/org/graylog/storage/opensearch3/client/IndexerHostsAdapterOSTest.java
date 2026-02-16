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
package org.graylog.storage.opensearch3.client;

import com.google.common.io.Resources;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexerHostsAdapterOSTest {

    @Test
    void returnsActiveHostsWithHttpScheme() {
        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/http", Resources.getResource("nodes_http.json"))
                .build();
        final IndexerHostsAdapterOS adapter = new IndexerHostsAdapterOS(client, List.of(URI.create("http://localhost:9200")));

        final List<URI> hosts = adapter.getActiveHosts();

        assertThat(hosts).containsExactlyInAnyOrder(
                URI.create("http://172.18.0.2:9200"),
                URI.create("http://172.18.0.3:9200")
        );
    }

    @Test
    void returnsActiveHostsWithHttpsScheme() {
        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/http", Resources.getResource("nodes_http.json"))
                .build();
        final IndexerHostsAdapterOS adapter = new IndexerHostsAdapterOS(client, List.of(URI.create("https://localhost:9200")));

        final List<URI> hosts = adapter.getActiveHosts();

        assertThat(hosts).containsExactlyInAnyOrder(
                URI.create("https://172.18.0.2:9200"),
                URI.create("https://172.18.0.3:9200")
        );
    }

    @Test
    void preservesSchemeFromPublishAddress() {
        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/http", Resources.getResource("nodes_http_with_scheme.json"))
                .build();
        final IndexerHostsAdapterOS adapter = new IndexerHostsAdapterOS(client, List.of(URI.create("http://localhost:9200")));

        final List<URI> hosts = adapter.getActiveHosts();

        assertThat(hosts).containsExactly(URI.create("https://172.18.0.2:9200"));
    }

    @Test
    void handlesHostnameSlashIpPortFormat() {
        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/http", Resources.getResource("nodes_http_with_hostname.json"))
                .build();
        final IndexerHostsAdapterOS adapter = new IndexerHostsAdapterOS(client, List.of(URI.create("https://localhost:9200")));

        final List<URI> hosts = adapter.getActiveHosts();

        assertThat(hosts).containsExactlyInAnyOrder(
                URI.create("https://172.18.0.2:9200"),
                URI.create("https://172.18.0.3:9200")
        );
    }

    @Test
    void defaultsToHttpWhenNoHostsConfigured() {
        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/http", Resources.getResource("nodes_http.json"))
                .build();
        final IndexerHostsAdapterOS adapter = new IndexerHostsAdapterOS(client, List.of());

        final List<URI> hosts = adapter.getActiveHosts();

        assertThat(hosts).containsExactlyInAnyOrder(
                URI.create("http://172.18.0.2:9200"),
                URI.create("http://172.18.0.3:9200")
        );
    }
}
