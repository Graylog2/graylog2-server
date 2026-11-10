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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.security.IndexerAdminCert;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.VersionAwareProvider;

import java.util.Map;

/**
 * Version-aware provider for the {@code @AdminIndexer IndicesAdapter} binding. Mirrors
 * {@link IndicesAdapterProvider} but reads from the qualifier-annotated map populated by each
 * storage backend module via {@code bindForVersion(IndicesAdapter.class, AdminIndexer.class)}.
 */
public class AdminIndicesAdapterProvider extends VersionAwareProvider<IndicesAdapter> {
    @Inject
    public AdminIndicesAdapterProvider(@DetectedSearchVersion SearchVersion version,
                                       @IndexerAdminCert Map<SearchVersion, Provider<IndicesAdapter>> pluginBindings) {
        super(version, pluginBindings);
    }
}
