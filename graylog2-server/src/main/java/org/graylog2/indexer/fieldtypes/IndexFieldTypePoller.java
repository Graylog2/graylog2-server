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
import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.mapping.GetMapping;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indices.Indices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class can be used to poll index field type information for indices in an {@link IndexSet}.
 */
public class IndexFieldTypePoller {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePoller.class);

    private final JestClient jestClient;
    private final Indices indices;
    private final Timer pollTimer;

    @Inject
    public IndexFieldTypePoller(final JestClient jestClient, final Indices indices, final MetricRegistry metricRegistry) {
        this.jestClient = jestClient;
        this.indices = indices;

        this.pollTimer = metricRegistry.timer(name(getClass(), "indexPollTime"));
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
    public Set<IndexFieldTypes> poll(final IndexSet indexSet, final Set<IndexFieldTypes> existingIndexTypes) {
        final String activeWriteIndex = indexSet.getActiveWriteIndex();
        final Set<String> existingIndexNames = existingIndexTypes.stream()
                .map(IndexFieldTypes::indexName)
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
    public Optional<IndexFieldTypes> pollIndex(final String indexName, final String indexSetId) {
        final GetMapping getMapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        final JestResult result;
        try (final Timer.Context ignored = pollTimer.time()) {
            result = JestUtils.execute(jestClient, getMapping, () -> "Unable to get index mapping for index: " + indexName);
        } catch (Exception e) {
            LOG.error("Couldn't get mapping for index <{}>", indexName, e);
            return Optional.empty();
        }

        final JsonNode properties = result.getJsonObject()
                .path(indexName)
                .path("mappings")
                .path("message")
                .path("properties");

        if (properties.isMissingNode()) {
            LOG.info("Invalid mapping response: {}", result.getJsonString());
            return Optional.empty();
        }

        final Spliterator<Map.Entry<String, JsonNode>> fieldSpliterator = Spliterators.spliteratorUnknownSize(properties.fields(), Spliterator.IMMUTABLE);

        final Map<String, FieldType> fieldsMap = StreamSupport.stream(fieldSpliterator, false)
                .map(field -> FieldType.create(field.getKey(), field.getValue().path("type").asText()))
                .collect(Collectors.toMap(FieldType::fieldName, Function.identity()));

        return Optional.of(IndexFieldTypes.create(indexSetId, indexName, fieldsMap));
    }
}
