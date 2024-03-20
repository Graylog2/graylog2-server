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
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Template;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.streams.Stream;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStreamServiceImpl implements DataStreamService {

    private static final Map<String, String> TIMESTAMP_TYPE = Map.of(
            "type", "date",
            "format", "yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis"
    );
    private final DataStreamAdapter dataStreamAdapter;
    private final IndexFieldTypesService indexFieldTypesService;
    private final int replicas;


    @Inject
    public DataStreamServiceImpl(DataStreamAdapter dataStreamAdapter, IndexFieldTypesService indexFieldTypesService,
                                 ClusterConfigService clusterConfigService) {
        this(dataStreamAdapter, indexFieldTypesService, clusterConfigService.get(IndexSetsDefaultConfiguration.class).replicas());
    }

    public DataStreamServiceImpl(DataStreamAdapter dataStreamAdapter, IndexFieldTypesService indexFieldTypesService,
                                 int replicas) {
        this.dataStreamAdapter = dataStreamAdapter;
        this.indexFieldTypesService = indexFieldTypesService;
        this.replicas = replicas;
    }

    @Override
    public void createDataStream(String dataStreamName, String timestampField,  Map<String, Map<String, String>> mappings,
                                 Policy ismPolicy) {
        updateDataStreamTemplate(dataStreamName, timestampField, mappings);
        dataStreamAdapter.createDataStream(dataStreamName);
        dataStreamAdapter.applyIsmPolicy(dataStreamName, ismPolicy);
        dataStreamAdapter.setNumberOfReplicas(dataStreamName, replicas);
    }

    private void updateDataStreamTemplate(String dataStreamName, String timestampField, Map<String, Map<String, String>> mappings) {
        final Map<String, Map<String, String>> effectiveMappings = mappings.containsKey(timestampField)
                ? mappings
                : ImmutableMap.<String, Map<String, String>>builder().putAll(mappings).put(timestampField, TIMESTAMP_TYPE).build();
        Template template = new Template(List.of(dataStreamName + "*"),
                new Template.Mappings(ImmutableMap.of("properties", effectiveMappings)), 99999L, new Template.Settings(ImmutableMap.of("number_of_replicas", replicas)));
        dataStreamAdapter.ensureDataStreamTemplate(dataStreamName + "-template", template, timestampField);
        createFieldTypes(dataStreamName, effectiveMappings);
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
