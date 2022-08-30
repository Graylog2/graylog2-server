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
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.Configuration;
import org.graylog2.indexer.fieldtypes.streamfiltered.filters.StreamBasedFieldTypeFilter;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;
import static org.graylog2.indexer.fieldtypes.streamfiltered.config.Config.MAX_FIELDS_TO_FILTER_AD_HOC;

public class MappedFieldTypesServiceImpl implements MappedFieldTypesService {
    private static final FieldTypes.Type UNKNOWN_TYPE = createType("unknown", of());
    private static final String PROP_COMPOUND_TYPE = "compound";

    private final StreamService streamService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final FieldTypeMapper fieldTypeMapper;
    private final IndexLookup indexLookup;
    private final boolean maintainsStreamBasedFieldLists;

    private final StreamBasedFieldTypeFilter adHocSearchEngineStreamBasedFieldTypeFilter;
    private final StreamBasedFieldTypeFilter allowAllStreamBasedFieldTypeFilter;
    private final StreamBasedFieldTypeFilter storedSearchEngineStreamBasedFieldTypeFilter;

    @Inject
    public MappedFieldTypesServiceImpl(final Configuration configuration,
                                       final StreamService streamService,
                                       final IndexFieldTypesService indexFieldTypesService,
                                       final FieldTypeMapper fieldTypeMapper,
                                       final IndexLookup indexLookup,
                                       @Named("AdHocFilter") final StreamBasedFieldTypeFilter adHocSearchEngineStreamBasedFieldTypeFilter,
                                       @Named("AllowAllFilter") final StreamBasedFieldTypeFilter allowAllStreamBasedFieldTypeFilter,
                                       @Named("StoredFilter") final StreamBasedFieldTypeFilter storedSearchEngineStreamBasedFieldTypeFilter) {
        this.streamService = streamService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.fieldTypeMapper = fieldTypeMapper;
        this.indexLookup = indexLookup;
        this.adHocSearchEngineStreamBasedFieldTypeFilter = adHocSearchEngineStreamBasedFieldTypeFilter;
        this.allowAllStreamBasedFieldTypeFilter = allowAllStreamBasedFieldTypeFilter;
        this.storedSearchEngineStreamBasedFieldTypeFilter = storedSearchEngineStreamBasedFieldTypeFilter;
        this.maintainsStreamBasedFieldLists = configuration.maintainsStreamBasedFieldLists();
    }

    @Override
    public Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Collection<String> streamIds, TimeRange timeRange) {
        final Set<String> indexSets = streamService.indexSetIdsByIds(streamIds);
        final Set<String> indexNames = this.indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.copyOf(streamIds), timeRange);
        final Set<FieldTypeDTO> fieldTypeDTOs = this.indexFieldTypesService.findForIndexSets(indexSets)
                .stream()
                .filter(fieldTypes -> indexNames.contains(fieldTypes.indexName()))
                .flatMap(fieldTypes -> fieldTypes.fields().stream())
                .collect(Collectors.toSet());

        StreamBasedFieldTypeFilter streamBasedFieldTypeFilter;
        if (!maintainsStreamBasedFieldLists) {
            streamBasedFieldTypeFilter = allowAllStreamBasedFieldTypeFilter;
        } else if (fieldTypeDTOs.size() < MAX_FIELDS_TO_FILTER_AD_HOC) {
            streamBasedFieldTypeFilter = adHocSearchEngineStreamBasedFieldTypeFilter;
        } else {
            streamBasedFieldTypeFilter = storedSearchEngineStreamBasedFieldTypeFilter;
        }

        try {
            final java.util.stream.Stream<MappedFieldTypeDTO> types = streamBasedFieldTypeFilter.filterFieldTypes(fieldTypeDTOs, indexNames, streamIds)
                    .stream()
                    .map(this::mapPhysicalFieldType);
            return mergeCompoundFieldTypes(types);
        } catch (Exception ex) {
            final java.util.stream.Stream<MappedFieldTypeDTO> types = allowAllStreamBasedFieldTypeFilter.filterFieldTypes(fieldTypeDTOs, indexNames, streamIds)
                    .stream()
                    .map(this::mapPhysicalFieldType);
            return mergeCompoundFieldTypes(types);
        }
    }

    private MappedFieldTypeDTO mapPhysicalFieldType(FieldTypeDTO fieldType) {
        final FieldTypes.Type mappedFieldType = fieldTypeMapper.mapType(fieldType).orElse(UNKNOWN_TYPE);
        return MappedFieldTypeDTO.create(fieldType.fieldName(), mappedFieldType);
    }

    private Set<MappedFieldTypeDTO> mergeCompoundFieldTypes(java.util.stream.Stream<MappedFieldTypeDTO> stream) {
        return stream.collect(Collectors.groupingBy(MappedFieldTypeDTO::name, Collectors.toSet()))
                .entrySet()
                .stream()
                .map(entry -> {
                    final Set<MappedFieldTypeDTO> fieldTypes = entry.getValue();
                    final String fieldName = entry.getKey();
                    if (fieldTypes.size() == 1) {
                        return fieldTypes.iterator().next();
                    }

                    final Set<String> distinctTypes = fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().type())
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    final String resultingFieldType = distinctTypes.size() > 1
                            ? distinctTypes.stream().collect(Collectors.joining(",", "compound(", ")"))
                            : distinctTypes.stream().findFirst().orElse("unknown");
                    final Set<String> commonProperties = fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().properties())
                            .reduce((s1, s2) -> Sets.intersection(s1, s2).immutableCopy())
                            .orElse(ImmutableSet.of());

                    final Set<String> properties = distinctTypes.size() > 1
                            ? Sets.union(commonProperties, Collections.singleton(PROP_COMPOUND_TYPE))
                            : commonProperties;
                    return MappedFieldTypeDTO.create(fieldName, createType(resultingFieldType, properties));

                })
                .collect(Collectors.toSet());

    }
}
