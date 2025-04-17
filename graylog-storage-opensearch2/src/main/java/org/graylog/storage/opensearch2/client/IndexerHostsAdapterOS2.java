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
package org.graylog.storage.opensearch2.client;

import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.indexer.client.IndexerHostsAdapter;

import java.net.URI;
import java.util.List;

public class IndexerHostsAdapterOS2 implements IndexerHostsAdapter {
    private final RestHighLevelClient client;

    @Inject
    public IndexerHostsAdapterOS2(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public List<URI> getActiveHosts() {
        return client.getLowLevelClient().getNodes().stream()
                .map(Node::getHost)
                .map(HttpHost::toURI)
                .map(URI::create)
                .toList();
    }
}
