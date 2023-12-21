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
package org.graylog.storage.elasticsearch7;

import com.google.common.collect.ImmutableSet;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.storage.SearchVersion;

import jakarta.inject.Inject;

import java.util.Collection;

import static org.graylog2.indexer.Constants.COMPOSABLE_INDEX_TEMPLATES_FEATURE;

public class Elasticsearch7Plugin implements Plugin {
    public static final SearchVersion SUPPORTED_ES_VERSION = SearchVersion.elasticsearch(7, 0, 0);
    public static final SearchVersion SUPPORTED_OPENSEARCH_VERSION = SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(1, 0, 0));

    @Inject
    private FeatureFlags featureFlags;

    @Override
    public PluginMetaData metadata() {
        return new Elasticsearch7Metadata();
    }

    @Override
    public Collection<PluginModule> modules() {
        var useComposableIndexTemplates = featureFlags.isOn(COMPOSABLE_INDEX_TEMPLATES_FEATURE);
        return ImmutableSet.of(
                new Elasticsearch7Module(SUPPORTED_ES_VERSION, useComposableIndexTemplates),
                new ViewsESBackendModule(SUPPORTED_ES_VERSION),
                new Elasticsearch7Module(SUPPORTED_OPENSEARCH_VERSION, useComposableIndexTemplates),
                new ViewsESBackendModule(SUPPORTED_OPENSEARCH_VERSION)
        );
    }
}
