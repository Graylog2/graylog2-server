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
package org.graylog2.indexer.fieldtypes.streamfiltered.module;

import com.google.inject.name.Names;
import org.graylog2.indexer.fieldtypes.streamfiltered.filters.AdHocSearchEngineStreamBasedFieldTypeFilter;
import org.graylog2.indexer.fieldtypes.streamfiltered.filters.AllowAllStreamBasedFieldTypeFilter;
import org.graylog2.indexer.fieldtypes.streamfiltered.filters.StoredSearchEngineStreamBasedFieldTypeFilter;
import org.graylog2.indexer.fieldtypes.streamfiltered.filters.StreamBasedFieldTypeFilter;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.StoredStreamFieldsService;
import org.graylog2.plugin.inject.Graylog2Module;

public class StreamFieldTypesModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(StreamBasedFieldTypeFilter.class)
                .annotatedWith(Names.named("AllowAllFilter"))
                .to(AllowAllStreamBasedFieldTypeFilter.class)
                .asEagerSingleton();
        bind(StreamBasedFieldTypeFilter.class)
                .annotatedWith(Names.named("AdHocFilter"))
                .to(AdHocSearchEngineStreamBasedFieldTypeFilter.class)
                .asEagerSingleton();
        bind(StreamBasedFieldTypeFilter.class)
                .annotatedWith(Names.named("StoredFilter"))
                .to(StoredSearchEngineStreamBasedFieldTypeFilter.class)
                .asEagerSingleton();

        bind(StoredStreamFieldsService.class).asEagerSingleton();
    }

}
