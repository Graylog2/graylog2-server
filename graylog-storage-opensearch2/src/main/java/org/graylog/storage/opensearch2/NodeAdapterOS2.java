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
package org.graylog.storage.opensearch2;

import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;

import jakarta.inject.Inject;

import java.util.Optional;

public class NodeAdapterOS2 implements NodeAdapter {
    private final OpenSearchClient client;

    @Inject
    public NodeAdapterOS2(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<SearchVersion> version() {
        var info = client.execute(c -> c.info());

        final Optional<String> version = Optional.ofNullable(info.version().number());

        final SearchVersion.Distribution distribution = Optional.ofNullable(info.version().distribution())
                .map(StringUtils::toUpperCase)
                .map(SearchVersion.Distribution::valueOf)
                .orElse(SearchVersion.Distribution.ELASTICSEARCH);

        return version
                .map(this::parseVersion)
                .map(v -> SearchVersion.create(distribution, v));
    }


}
