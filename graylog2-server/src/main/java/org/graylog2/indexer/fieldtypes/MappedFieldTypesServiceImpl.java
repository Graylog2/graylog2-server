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
package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class MappedFieldTypesServiceImpl implements MappedFieldTypesService {
    private final StreamService streamService;
    private final IndexFieldTypesService indexFieldTypesService;

    private final IndexLookup indexLookup;
    private final FieldTypesMerger fieldTypesMerger;


    @Inject
    public MappedFieldTypesServiceImpl(StreamService streamService,
                                       IndexFieldTypesService indexFieldTypesService,
                                       FieldTypesMerger fieldTypesMerger,
                                       IndexLookup indexLookup) {
        this.streamService = streamService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.fieldTypesMerger = fieldTypesMerger;
        this.indexLookup = indexLookup;
    }

    @Override
    public Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Collection<String> streamIds, TimeRange timeRange) {
        final Set<String> indexSets = streamService.indexSetIdsByIds(streamIds);

        final Set<String> indexNames = this.indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.copyOf(streamIds), timeRange);

        final java.util.stream.Stream<MappedFieldTypeDTO> types = this.indexFieldTypesService.findForIndexSets(indexSets)
                .stream()
                .filter(fieldTypes -> indexNames.contains(fieldTypes.indexName()))
                .flatMap(fieldTypes -> fieldTypes.fields().stream())
                .map(fieldTypesMerger::mapPhysicalFieldType);
        return fieldTypesMerger.mergeCompoundFieldTypes(types);
    }


}
