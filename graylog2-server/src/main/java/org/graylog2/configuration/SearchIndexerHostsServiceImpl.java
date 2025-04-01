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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.client.IndexerHostsAdapter;

import java.net.URI;
import java.util.List;

public class SearchIndexerHostsServiceImpl implements SearchIndexerHostsService {

    private final List<URI> initialHosts;
    private final List<URI> configuredHosts;
    private final IndexerHostsAdapter indexerHostsAdapter;

    @Inject
    public SearchIndexerHostsServiceImpl(
            @IndexerHosts List<URI> initialHosts,
            @Named("elasticsearch_hosts") List<URI> configuredHosts,
            IndexerHostsAdapter indexerHostsAdapter
    ) {
        this.initialHosts = initialHosts;
        this.configuredHosts = configuredHosts;
        this.indexerHostsAdapter = indexerHostsAdapter;
    }

    @Override
    public SearchIndexerHosts getHosts() {
        return new SearchIndexerHosts(configuredHosts, initialHosts, indexerHostsAdapter.getCurrentHosts());
    }
}
