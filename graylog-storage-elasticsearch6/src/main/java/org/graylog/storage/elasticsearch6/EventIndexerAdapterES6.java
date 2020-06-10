package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventDto;
import org.graylog.events.indices.EventIndexerAdapter;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.joda.time.DateTimeZone.UTC;

public class EventIndexerAdapterES6 implements EventIndexerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(EventIndexerAdapterES6.class);
    private final JestClient jestClient;
    private final ObjectMapper objectMapper;

    @Inject
    public EventIndexerAdapterES6(JestClient jestClient, ObjectMapper objectMapper) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void bulkIndex(List<Map.Entry<String, Event>> requests) {
        final Bulk.Builder bulk = new Bulk.Builder();
        for (final Map.Entry<String, Event> entry : requests) {
            final Event event = entry.getValue();

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

            final String index = entry.getKey();
            bulk.addAction(new Index.Builder(source)
                    .index(index)
                    .type(IndexMapping.TYPE_MESSAGE)
                    .id(event.getId())
                    .build());
        }

        try {
            final BulkResult result = jestClient.execute(bulk.build());
            final List<BulkResult.BulkResultItem> failedItems = result.getFailedItems();

            if (!failedItems.isEmpty()) {
                LOG.error("Failed to index {} events: {}", requests.size(), result.getErrorMessage());
            }
            LOG.debug("Index: Bulk indexed {} events, failures: {}", result.getItems().size(), failedItems.size());
        } catch (IOException e) {
            LOG.error("Failed to index {} events", requests.size(), e);
        }
    }
}
