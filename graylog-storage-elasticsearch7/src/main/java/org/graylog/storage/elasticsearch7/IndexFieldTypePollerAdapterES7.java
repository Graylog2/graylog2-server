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
package org.graylog.storage.elasticsearch7;

import com.codahale.metrics.Timer;
import org.graylog.storage.elasticsearch7.mapping.FieldMappingApi;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexFieldTypePollerAdapterES7 implements IndexFieldTypePollerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePollerAdapterES7.class);
    private final ElasticsearchClient client;
    private final FieldMappingApi fieldMappingApi;

    @Inject
    public IndexFieldTypePollerAdapterES7(ElasticsearchClient client,
                                          FieldMappingApi fieldMappingApi) {
        this.client = client;
        this.fieldMappingApi = fieldMappingApi;
    }

    @Override
    public Optional<Set<FieldTypeDTO>> pollIndex(String indexName, Timer pollTimer) {
        final Map<String, String> fieldTypes;
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

        return Optional.of(fieldTypes.entrySet()
                        .stream()
                        // The "type" value is empty if we deal with a nested data type
                        // TODO: Figure out how to handle nested fields, for now we only support the top-level fields
                        .filter(field -> !field.getValue().isEmpty())
                        .map(field -> FieldTypeDTO.create(field.getKey(), field.getValue()))
                        .collect(Collectors.toSet())
        );
    }
}
