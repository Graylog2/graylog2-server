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

import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.indexer.client.IndexerHostsAdapter;
import org.opensearch.client.opensearch.nodes.NodesInfoResponse;
import org.opensearch.client.opensearch.nodes.info.NodeInfo;
import org.opensearch.client.opensearch.nodes.info.NodeInfoHttp;
import org.opensearch.client.opensearch.nodes.info.NodesInfoMetric;

import java.net.URI;
import java.util.List;
import java.util.Objects;

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
        final NodesInfoResponse response = client.sync(
                c -> c.nodes().info(r -> r.metric(NodesInfoMetric.Http)),
                "Unable to retrieve indexer hosts"
        );
        return response.nodes().values().stream()
                .map(NodeInfo::http)
                .filter(Objects::nonNull)
                .map(NodeInfoHttp::publishAddress)
                .map(this::toURI)
                .toList();
    }

    private URI toURI(String address) {
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return URI.create(address);
        }
        return URI.create(f("%s://%s", defaultScheme, address));
    }
}
