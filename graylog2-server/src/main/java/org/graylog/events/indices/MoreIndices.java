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
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.joda.time.DateTimeZone.UTC;

/**
 * This class contains indices helper for the events system.
 */
@Singleton
public class MoreIndices {
    private static final Logger LOG = LoggerFactory.getLogger(MoreIndices.class);
    private static final String EVENTS_INDEX_PREFIX = "gl-events";
    private static final String EVENTS_WRITE_INDEX_ALIAS = EVENTS_INDEX_PREFIX + MongoIndexSet.SEPARATOR + MongoIndexSet.DEFLECTOR_SUFFIX;

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;

    @Inject
    public MoreIndices(JestClient jestClient, ObjectMapper objectMapper) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
    }

    public void bulkIndex(List<EventWithContext> eventsWithContext) {
        if (eventsWithContext.isEmpty()) {
            return;
        }

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

            bulk.addAction(new Index.Builder(source)
                    .index(EVENTS_WRITE_INDEX_ALIAS) // TODO: Get write index alias from index set
                    .type(IndexMapping.TYPE_MESSAGE)
                    .id(event.getId())
                    .build());
        }

        try {
            final BulkResult result = jestClient.execute(bulk.build());
            final List<BulkResult.BulkResultItem> failedItems = result.getFailedItems();

            if (!failedItems.isEmpty()) {
                LOG.error("Failed to index {} events: {}", eventsWithContext.size(), result.getErrorMessage());
            }
            LOG.info("Index: Bulk indexed {} events, failures: {}", result.getItems().size(), failedItems.size());
        } catch (IOException e) {
            LOG.error("Failed to index {} events", eventsWithContext.size(), e);
        }
    }
}
