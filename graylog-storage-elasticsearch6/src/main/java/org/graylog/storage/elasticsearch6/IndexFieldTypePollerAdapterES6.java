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
package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.mapping.GetMapping;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IndexFieldTypePollerAdapterES6 implements IndexFieldTypePollerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePollerAdapterES6.class);
    private final JestClient jestClient;

    @Inject
    public IndexFieldTypePollerAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public Optional<Set<FieldTypeDTO>> pollIndex(String indexName, Timer pollTimer) {
        final GetMapping getMapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        final JestResult result;
        try (final Timer.Context ignored = pollTimer.time()) {
            result = JestUtils.execute(jestClient, getMapping, () -> "Unable to get index mapping for index: " + indexName);
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Couldn't get mapping for index <{}>", indexName, e);
            } else {
                LOG.error("Couldn't get mapping for index <{}>: {}", indexName, ExceptionUtils.getRootCauseMessage(e));
            }
            return Optional.empty();
        }

        final JsonNode properties = result.getJsonObject()
                .path(indexName)
                .path("mappings")
                .path(IndexMapping.TYPE_MESSAGE)
                .path("properties");

        if (properties.isMissingNode()) {
            LOG.error("Invalid mapping response: {}", result.getJsonString());
            return Optional.empty();
        }

        final Spliterator<Map.Entry<String, JsonNode>> fieldSpliterator = Spliterators.spliteratorUnknownSize(properties.fields(), Spliterator.IMMUTABLE);

        final Map<String, String> fieldTypes = StreamSupport.stream(fieldSpliterator, false)
                .collect(Collectors.toMap(Map.Entry::getKey, field -> field.getValue().path("type").asText()));
        return Optional.of(
                fieldTypes.entrySet()
                        .stream()
                        // The "type" value is empty if we deal with a nested data type
                        // TODO: Figure out how to handle nested fields, for now we only support the top-level fields
                        .filter(field -> !field.getValue().isEmpty())
                        .map(field -> FieldTypeDTO.create(field.getKey(), field.getValue()))
                        .collect(Collectors.toSet())
        );
    }
}
