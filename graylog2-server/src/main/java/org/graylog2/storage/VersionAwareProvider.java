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
package org.graylog2.storage;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class VersionAwareProvider<T> implements Provider<T> {
    private final SearchVersion elasticsearchMajorVersion;
    private final Map<SearchVersion, Provider<T>> pluginBindings;

    @Inject
    public VersionAwareProvider(@DetectedSearchVersion SearchVersion elasticsearchVersion, Map<SearchVersion, Provider<T>> pluginBindings) {
        this.elasticsearchMajorVersion = majorVersionFrom(elasticsearchVersion);
        this.pluginBindings = pluginBindings;
    }

    @Override
    public T get() {
        final Provider<T> provider = this.pluginBindings.get(elasticsearchMajorVersion);
        if (provider == null) {
            throw new UnsupportedSearchException(elasticsearchMajorVersion, this.getClass().getName());
        }
        return provider.get();
    }

    private SearchVersion majorVersionFrom(SearchVersion version) {
        return version.major();
    }
}
