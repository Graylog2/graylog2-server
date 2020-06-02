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

import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains indices helper for the events system.
 */
@Singleton
public class MoreIndices {
    private static final Logger LOG = LoggerFactory.getLogger(MoreIndices.class);

    private final StreamService streamService;
    private final MoreIndicesAdapter moreIndicesAdapter;

    @Inject
    public MoreIndices(StreamService streamService, MoreIndicesAdapter moreIndicesAdapter) {
        this.streamService = streamService;
        this.moreIndicesAdapter = moreIndicesAdapter;
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
        final List<Map.Entry<String, Event>> requests = eventsWithContext.stream()
                .flatMap(eventWithContext -> {
                    final Event event = eventWithContext.event();
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
                    return indices.stream()
                            .map(index -> new AbstractMap.SimpleEntry<>(index, event));
                })
                .collect(Collectors.toList());
        moreIndicesAdapter.bulkIndex(requests);
    }
}
