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
package org.graylog2.storage.providers;

import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.plugin.Version;
import org.graylog2.storage.ElasticsearchVersion;
import org.graylog2.storage.VersionAwareProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class IndexFieldTypePollerAdapterProvider extends VersionAwareProvider<IndexFieldTypePollerAdapter> {
    @Inject
    public IndexFieldTypePollerAdapterProvider(@ElasticsearchVersion Version version, Map<Version, Provider<IndexFieldTypePollerAdapter>> pluginBindings) {
        super(version, pluginBindings);
    }
}
