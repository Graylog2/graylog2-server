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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.indexer.client.IndexerHostsAdapter;
import org.opensearch.client.opensearch.generic.Requests;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.graylog2.shared.utilities.StringUtils.f;

public class IndexerHostsAdapterOS implements IndexerHostsAdapter {
    private final OfficialOpensearchClient client;
    private final String defaultScheme;

    @Inject
    public IndexerHostsAdapterOS(OfficialOpensearchClient client, @IndexerHosts List<URI> hosts) {
        this.client = client;
        this.defaultScheme = hosts.stream().findFirst().map(URI::getScheme).orElse("http");
    }

    @Override
    public List<URI> getActiveHosts() {
        final JsonNode response = client.performRequest(
                Requests.builder().endpoint("/_nodes/http").method("GET").build(),
                "Unable to retrieve indexer hosts"
        );
        final JsonNode nodes = response.path("nodes");
        final Iterator<String> nodeIds = nodes.fieldNames();
        return StreamSupport.stream(((Iterable<String>) () -> nodeIds).spliterator(), false)
                .map(nodeId -> nodes.path(nodeId).path("http").path("publish_address").asText())
                .filter(addr -> !addr.isEmpty())
                .map(this::toURI)
                .toList();
    }

    private URI toURI(String address) {
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return URI.create(address);
        }
        // publish_address can be in "hostname/ip:port" format — use the ip:port part
        final int slashIndex = address.indexOf('/');
        final String hostPort = slashIndex >= 0 ? address.substring(slashIndex + 1) : address;
        return URI.create(f("%s://%s", defaultScheme, hostPort));
    }
}
