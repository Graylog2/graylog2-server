/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.fieldtypes;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;

import javax.inject.Inject;
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

    @Inject
    public IndexFieldTypePoller(final Indices indices, final MetricRegistry metricRegistry, IndexFieldTypePollerAdapter indexFieldTypePollerAdapter) {
        this.indices = indices;

        this.pollTimer = metricRegistry.timer(name(getClass(), "indexPollTime"));
        this.indexFieldTypePollerAdapter = indexFieldTypePollerAdapter;
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
                .filter(indexName -> indexName.equals(activeWriteIndex) || !existingIndexNames.contains(indexName))
                .map(indexName -> pollIndex(indexName, indexSet.getConfig().id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the index field types for the given index.
     *
     * @param indexName index name to poll types for
     * @param indexSetId index set ID of the given index
     * @return the polled index field type data for the given index
     */
    public Optional<IndexFieldTypesDTO> pollIndex(final String indexName, final String indexSetId) {
        final Optional<Set<FieldTypeDTO>> optionalFields = indexFieldTypePollerAdapter.pollIndex(indexName, pollTimer)
                .map(fieldTypes -> fieldTypes
                    .entrySet()
                    .stream()
                    // The "type" value is empty if we deal with a nested data type
                    // TODO: Figure out how to handle nested fields, for now we only support the top-level fields
                    .filter(field -> !field.getValue().isEmpty())
                    .map(field -> FieldTypeDTO.create(field.getKey(), field.getValue()))
                    .collect(Collectors.toSet()));

        return optionalFields.map(fields -> IndexFieldTypesDTO.create(indexSetId, indexName, fields));
    }
}
