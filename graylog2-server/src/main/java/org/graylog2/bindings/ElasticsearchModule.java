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
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.plugin.Version;
import org.graylog2.storage.ElasticsearchVersion;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;

public class ElasticsearchModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Version.class).annotatedWith(ElasticsearchVersion.class).toProvider(ElasticsearchVersionProvider.class).asEagerSingleton();
        bind(IndexMappingFactory.class).asEagerSingleton();
    }
}
