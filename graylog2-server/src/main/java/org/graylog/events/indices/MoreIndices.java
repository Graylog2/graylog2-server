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
package org.graylog.events.indices;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventDto;
import org.graylog.events.event.EventWithContext;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.joda.time.DateTimeZone.UTC;

/**
 * This class contains indices helper for the events system.
 */
@Singleton
public class MoreIndices {
    private static final Logger LOG = LoggerFactory.getLogger(MoreIndices.class);

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    @Inject
    public MoreIndices(JestClient jestClient, ObjectMapper objectMapper, StreamService streamService) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.streamService = streamService;
    }

    public void bulkIndex(List<EventWithContext> eventsWithContext) {
        if (eventsWithContext.isEmpty()) {
            return;
        }

        // Pre-load all write index targets of all events to avoid looking them up for every event when building the bulk request
        final Set<String> streamIds = eventsWithContext.stream()
            .map(EventWithContext::event)
            .flatMap(event -> event.getStreams().stream())
            .collect(Collectors.toSet());
        final Map<String, String> streamIndices = streamService.loadByIds(streamIds).stream()
            .collect(Collectors.toMap(Persisted::getId, stream -> stream.getIndexSet().getWriteIndexAlias()));

        final Bulk.Builder bulk = new Bulk.Builder();
        for (final EventWithContext eventWithContext : eventsWithContext) {
            final Event event = eventWithContext.event();

            final Map<String, Object> source = objectMapper.convertValue(event.toDto(), TypeReferences.MAP_STRING_OBJECT);

            // "Fix" timestamps to be in the correct format. Our message index mapping is using this format so we have
            // to use it for our events as well to make sure we can use the search without errors.
            source.put(EventDto.FIELD_EVENT_TIMESTAMP, buildElasticSearchTimeFormat(requireNonNull(event.getEventTimestamp()).withZone(UTC)));
            source.put(EventDto.FIELD_PROCESSING_TIMESTAMP, buildElasticSearchTimeFormat(requireNonNull(event.getProcessingTimestamp()).withZone(UTC)));
            if (event.getTimerangeStart() != null) {
                source.put(EventDto.FIELD_TIMERANGE_START, buildElasticSearchTimeFormat(event.getTimerangeStart().withZone(UTC)));
            }
            if (event.getTimerangeEnd() != null) {
                source.put(EventDto.FIELD_TIMERANGE_END, buildElasticSearchTimeFormat(event.getTimerangeEnd().withZone(UTC)));
            }

            // We cannot index events that don't have any stream set
            if (event.getStreams().isEmpty()) {
                throw new IllegalStateException("Event streams cannot be empty");
            }

            // Collect a set of indices for the event to avoid writing to the same index set twice if
            // multiple streams use the same index set.
            final Set<String> indices = event.getStreams().stream()
                .map(streamId -> {
                    final String index = streamIndices.get(streamId);
                    if (index == null) {
                        LOG.warn("Couldn't find index set of stream <{}> for event <{}> (definition: {}/{})", streamId,
                            event.getId(), event.getEventDefinitionType(), event.getEventDefinitionId());
                    }
                    return index;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            for (final String index : indices) {
                bulk.addAction(new Index.Builder(source)
                    .index(index)
                    .type(IndexMapping.TYPE_MESSAGE)
                    .id(event.getId())
                    .build());
            }
        }

        try {
            final BulkResult result = jestClient.execute(bulk.build());
            final List<BulkResult.BulkResultItem> failedItems = result.getFailedItems();

            if (!failedItems.isEmpty()) {
                LOG.error("Failed to index {} events: {}", eventsWithContext.size(), result.getErrorMessage());
            }
            LOG.debug("Index: Bulk indexed {} events, failures: {}", result.getItems().size(), failedItems.size());
        } catch (IOException e) {
            LOG.error("Failed to index {} events", eventsWithContext.size(), e);
        }
    }
}
