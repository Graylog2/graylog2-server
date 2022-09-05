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

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.storage.SearchVersion;

import java.util.Collection;

public class Opensearch2Plugin implements Plugin {
    public static final SearchVersion SUPPORTED_OPENSEARCH_VERSION = SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(2, 0, 0));

    @Override
    public PluginMetaData metadata() {
        return new Opensearch2Metadata();
    }

    @Override
    public Collection<PluginModule> modules() {
        return ImmutableSet.of(
                new Opensearch2Module(SUPPORTED_OPENSEARCH_VERSION),
                new ViewsOSBackendModule(SUPPORTED_OPENSEARCH_VERSION)
        );
    }
}
