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
package org.graylog.storage.opensearch2;

import com.codahale.metrics.Timer;
import com.google.common.collect.Iterables;
import org.graylog.storage.opensearch2.mapping.FieldMappingApi;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsWithFieldUsageRetriever;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexFieldTypePollerAdapterOS2 implements IndexFieldTypePollerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePollerAdapterOS2.class);
    private final FieldMappingApi fieldMappingApi;
    private final boolean maintainsStreamBasedFieldLists;
    private final StreamsWithFieldUsageRetriever streamsWithFieldUsageRetriever;

    @Inject
    public IndexFieldTypePollerAdapterOS2(final FieldMappingApi fieldMappingApi,
                                          final Configuration configuration,
                                          final StreamsWithFieldUsageRetriever streamsWithFieldUsageRetriever) {
        this.fieldMappingApi = fieldMappingApi;
        this.maintainsStreamBasedFieldLists = configuration.maintainsStreamBasedFieldLists();
        this.streamsWithFieldUsageRetriever = streamsWithFieldUsageRetriever;
    }

    @Override
    public Optional<Set<FieldTypeDTO>> pollIndex(String indexName, Timer pollTimer) {
        final Map<String, FieldMappingApi.FieldMapping> fieldTypes;
        try (final Timer.Context ignored = pollTimer.time()) {
            fieldTypes = fieldMappingApi.fieldTypes(indexName);
        } catch (IndexNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Couldn't get mapping for index <{}>", indexName, e);
            } else {
                LOG.error("Couldn't get mapping for index <{}>: {}", indexName, ExceptionUtils.getRootCauseMessage(e));
            }
            return Optional.empty();
        }

        final Map<String, FieldMappingApi.FieldMapping> filteredFieldTypes = fieldTypes.entrySet()
                .stream()
                // The "type" value is empty if we deal with a nested data type
                // TODO: Figure out how to handle nested fields, for now we only support the top-level fields
                .filter(field -> !field.getValue().type().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!maintainsStreamBasedFieldLists) {
            return Optional.of(filteredFieldTypes.entrySet()
                    .stream()
                    .map(field -> fromFieldNameAndMapping(field.getKey(), field.getValue())
                            .streams(Set.of())
                            .build())
                    .collect(Collectors.toSet()));
        } else {
            Set<FieldTypeDTO> result = new HashSet<>();
            final Iterable<List<Map.Entry<String, FieldMappingApi.FieldMapping>>> partitioned = Iterables.partition(filteredFieldTypes.entrySet(), MAX_SEARCHES_PER_MULTI_SEARCH);
            for (var batch : partitioned) {
                final Map<String, Set<String>> streams = streamsWithFieldUsageRetriever.getStreams(batch.stream().map(Map.Entry::getKey).collect(Collectors.toList()), indexName);
                batch.stream()
                        .map(entry -> fromFieldNameAndMapping(entry.getKey(), entry.getValue())
                                .streams(streams.get(entry.getKey()))
                                .build()
                        )
                        .forEach(result::add);

            }
            return Optional.of(result);
        }

    }

    private FieldTypeDTO.Builder fromFieldNameAndMapping(final String fieldName, final FieldMappingApi.FieldMapping mapping) {
        final Boolean fieldData = mapping.fielddata().orElse(false);
        return FieldTypeDTO.builder()
                .fieldName(fieldName)
                .physicalType(mapping.type())
                .properties(fieldData ?
                        Collections.singleton(FieldTypeDTO.Properties.FIELDDATA)
                        : Set.of());
    }

    @Override
    public boolean maintainsStreamBasedFieldLists() {
        return maintainsStreamBasedFieldLists;
    }
}
