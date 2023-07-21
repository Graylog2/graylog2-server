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
package org.graylog.testing.completebackend;

import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import java.util.Optional;
import java.util.ServiceLoader;

public class SearchServerInstanceFactoryByVersion {
    private static ServiceLoader<SearchServerInterfaceProvider> loader = ServiceLoader.load(SearchServerInterfaceProvider.class);
    private final SearchVersion version;

    public SearchServerInstanceFactoryByVersion(SearchVersion searchVersion) {
        this.version = searchVersion;
    }

    public static Optional<SearchServerBuilder> getSearchServerInterfaceBuilder(SearchVersion searchVersion) {
        for (SearchServerInterfaceProvider provider : loader) {
            SearchServerBuilder builder = provider.getBuilderFor(searchVersion);
            if (builder != null) {
                return Optional.of(builder);
            }
        }
        return Optional.empty();
    }

    public SearchServerInstance create(final Network network) {
        return getSearchServerInterfaceBuilder(this.version)
                .map(builder -> builder.network(network))
                .map(SearchServerBuilder::build)
                .orElseThrow(() -> new UnsupportedOperationException("Search version " + version + " not supported."));
    }

    public SearchVersion getVersion() {
        return version;
    }
}
