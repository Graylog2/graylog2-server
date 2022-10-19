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
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

public class MappedFieldTypesServiceImpl implements MappedFieldTypesService {
    private static final FieldTypes.Type UNKNOWN_TYPE = createType("unknown", of());
    private static final String PROP_COMPOUND_TYPE = "compound";

    private final StreamService streamService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final FieldTypeMapper fieldTypeMapper;
    private final IndexLookup indexLookup;
    private final boolean streamAwareFieldTypes;


    @Inject
    public MappedFieldTypesServiceImpl(final Configuration configuration,
                                       final StreamService streamService,
                                       final IndexFieldTypesService indexFieldTypesService,
                                       final FieldTypeMapper fieldTypeMapper,
                                       final IndexLookup indexLookup) {
        this.streamService = streamService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.fieldTypeMapper = fieldTypeMapper;
        this.indexLookup = indexLookup;
        this.streamAwareFieldTypes = configuration.maintainsStreamAwareFieldTypes();
    }

    @Override
    public Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Collection<String> streamIds, TimeRange timeRange) {
        final Set<String> indexSets = streamService.indexSetIdsByIds(streamIds);
        final Set<String> indexNames = this.indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.copyOf(streamIds), timeRange);
        final Set<FieldTypeDTO> fieldTypeDTOs = this.indexFieldTypesService.findForIndexSets(indexSets)
                .stream()
                .filter(fieldTypes -> indexNames.contains(fieldTypes.indexName()))
                .flatMap(fieldTypes -> fieldTypes.fields().stream())
                .filter(fieldTypeDTO -> !streamAwareFieldTypes || !Collections.disjoint(fieldTypeDTO.streams(), streamIds))
                .collect(Collectors.toSet());

        return mergeCompoundFieldTypes(fieldTypeDTOs.stream()
                .map(this::mapPhysicalFieldType));
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
