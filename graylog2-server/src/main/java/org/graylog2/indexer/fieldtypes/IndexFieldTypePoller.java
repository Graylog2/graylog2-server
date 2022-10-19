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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class can be used to poll index field type information for indices in an {@link IndexSet}.
 */
public class IndexFieldTypePoller {
    private final Indices indices;
    private final Timer pollTimer;
    private final IndexFieldTypePollerAdapter indexFieldTypePollerAdapter;
    private final boolean maintainsStreamBasedFieldLists;

    @Inject
    public IndexFieldTypePoller(final Indices indices,
                                final MetricRegistry metricRegistry,
                                final IndexFieldTypePollerAdapter indexFieldTypePollerAdapter) {
        this.indices = indices;

        this.pollTimer = metricRegistry.timer(name(getClass(), "indexPollTime"));
        this.indexFieldTypePollerAdapter = indexFieldTypePollerAdapter;
        this.maintainsStreamBasedFieldLists = indexFieldTypePollerAdapter.maintainsStreamBasedFieldLists();
    }

    /**
     * Returns the index field types for the given index set.
     * <p>
     * Indices present in <code>existingIndexTypes</code> (except for the current write index) will not be polled
     * again to avoid Elasticsearch requests.
     *
     * @param indexSet index set to poll
     * @param existingIndexTypes existing index field type data
     * @return the polled index field type data for the given index set
     */
    public Set<IndexFieldTypesDTO> poll(final IndexSet indexSet, final Set<IndexFieldTypesDTO> existingIndexTypes) {
        final String activeWriteIndex = indexSet.getActiveWriteIndex();
        final Set<String> existingIndexNames = existingIndexTypes.stream()
                .map(IndexFieldTypesDTO::indexName)
                .collect(Collectors.toSet());

        return indices.getIndices(indexSet, "open").stream()
                // We always poll the active write index because the mapping can change for every ingested message.
                // Other indices will only be polled if we don't have the mapping data already.
                .filter(indexName -> indexName.equals(activeWriteIndex) || !existingIndexNames.contains(indexName)
                        || (maintainsStreamBasedFieldLists && missesStreamData(existingIndexTypes, indexName)))
                .map(indexName -> pollIndex(indexName, indexSet.getConfig().id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private boolean missesStreamData(final Collection<IndexFieldTypesDTO> existingIndexTypes, final String indexName) {
        return existingIndexTypes.stream()
                .filter(x -> x.indexName().equals(indexName))
                .anyMatch(x -> !x.hasStreamData());
    }

    /**
     * Returns the index field types for the given index.
     *
     * @param indexName  index name to poll types for
     * @param indexSetId index set ID of the given index
     * @return the polled index field type data for the given index
     */
    public Optional<IndexFieldTypesDTO> pollIndex(final String indexName, final String indexSetId) {
        final Optional<Set<FieldTypeDTO>> optionalFields = indexFieldTypePollerAdapter.pollIndex(indexName, pollTimer);

        return optionalFields.map(fields -> IndexFieldTypesDTO.builder()
                .indexSetId(indexSetId)
                .indexName(indexName)
                .fields(fields)
                .hasStreamData(maintainsStreamBasedFieldLists)
                .build());
    }
}
