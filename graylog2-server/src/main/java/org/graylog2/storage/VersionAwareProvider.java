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

import org.graylog2.plugin.Version;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class VersionAwareProvider<T> implements Provider<T> {
    private final Version elasticsearchMajorVersion;
    private final Map<Version, Provider<T>> pluginBindings;

    @Inject
    public VersionAwareProvider(@ElasticsearchVersion Version elasticsearchVersion, Map<Version, Provider<T>> pluginBindings) {
        this.elasticsearchMajorVersion = majorVersionFrom(elasticsearchVersion);
        this.pluginBindings = pluginBindings;
    }

    @Override
    public T get() {
        final Provider<T> provider = this.pluginBindings.get(elasticsearchMajorVersion);
        if (provider == null) {
            throw new UnsupportedElasticsearchException(elasticsearchMajorVersion);
        }
        return provider.get();
    }

    private Version majorVersionFrom(Version version) {
        return Version.from(version.getVersion().getMajorVersion(), 0, 0);
    }
}
