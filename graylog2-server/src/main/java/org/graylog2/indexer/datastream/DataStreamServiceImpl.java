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
package org.graylog2.indexer.datastream;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Template;
import org.graylog2.plugin.streams.Stream;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStreamServiceImpl implements DataStreamService {

    private final DataStreamAdapter dataStreamAdapter;
    private final IndexFieldTypesService indexFieldTypesService;

    @Inject
    public DataStreamServiceImpl(DataStreamAdapter dataStreamAdapter, IndexFieldTypesService indexFieldTypesService) {
        this.dataStreamAdapter = dataStreamAdapter;
        this.indexFieldTypesService = indexFieldTypesService;
    }

    @Override
    public void createDataStream(String dataStreamName, String timestampField,  Map<String, Map<String, String>> mappings,
                                 Policy ismPolicy) {
        updateDataStreamTemplate(dataStreamName, timestampField, mappings);
        dataStreamAdapter.createDataStream(dataStreamName);
        dataStreamAdapter.applyIsmPolicy(dataStreamName, ismPolicy);
    }

    private void updateDataStreamTemplate(String dataStreamName, String timestampField, Map<String, Map<String, String>> mappings) {
        if (!mappings.containsKey(timestampField)) {
            mappings.put(timestampField, ImmutableMap.of(
                    "type", "date",
                    "format", "yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis")
            );
        }
        Template template = new Template(List.of(dataStreamName + "*"),
                new Template.Mappings(ImmutableMap.of("properties", mappings)), 99999L, new Template.Settings(Map.of()));
        dataStreamAdapter.ensureDataStreamTemplate(dataStreamName + "-template", template, timestampField);
        createFieldTypes(dataStreamName, mappings);
    }

    private void createFieldTypes(String metricsStream, Map<String, Map<String, String>> mappings) {
        final Set<FieldTypeDTO> fields = mappings.entrySet().stream()
                .map(mapping -> FieldTypeDTO.builder()
                        .fieldName(mapping.getKey())
                        .physicalType(mapping.getValue().get("type"))
                        .build())
                .collect(Collectors.toSet());
        IndexFieldTypesDTO dto = IndexFieldTypesDTO.create(Stream.DATASTREAM_PREFIX + metricsStream, metricsStream, fields);
        indexFieldTypesService.upsert(dto);
    }
}
