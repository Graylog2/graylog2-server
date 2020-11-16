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
package org.graylog.events.indices;

import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
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
import java.util.stream.Stream;

/**
 * This class contains indices helper for the events system.
 */
@Singleton
public class EventIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(EventIndexer.class);

    private final StreamService streamService;
    private final Messages messages;

    @Inject
    public EventIndexer(StreamService streamService, Messages messages) {
        this.streamService = streamService;
        this.messages = messages;
    }

    public void write(List<EventWithContext> eventsWithContext) {
        if (eventsWithContext.isEmpty()) {
            return;
        }

        // Pre-load all write index targets of all events to avoid looking them up for every event when building the bulk request
        final Set<String> streamIds = streamIdsForEvents(eventsWithContext);
        final Map<String, IndexSet> streamIndices = indexSetsForStreams(streamIds);
        final List<IndexingRequest> requests = eventsWithContext.stream()
                .map(EventWithContext::event)
                // Collect a set of indices for the event to avoid writing to the same index set twice if
                // multiple streams use the same index set.
                .flatMap(event -> assignEventsToTargetIndices(event, streamIndices))
                .map(event -> IndexingRequest.create(event.getKey(), event.getValue()))
                .collect(Collectors.toList());
        messages.bulkIndexRequests(requests, true);
    }

    private Map<String, IndexSet> indexSetsForStreams(Set<String> streamIds) {
        return streamService.loadByIds(streamIds).stream()
            .collect(Collectors.toMap(Persisted::getId, org.graylog2.plugin.streams.Stream::getIndexSet));
    }

    private Set<String> streamIdsForEvents(List<EventWithContext> eventsWithContext) {
        return eventsWithContext.stream()
            .map(EventWithContext::event)
            .flatMap(event -> event.getStreams().stream())
            .collect(Collectors.toSet());
    }

    private Stream<AbstractMap.SimpleEntry<IndexSet, Event>> assignEventsToTargetIndices(Event event, Map<String, IndexSet> streamIndices) {
        final Set<IndexSet> indices = indicesForEvent(event, streamIndices);
        return indices.stream()
                .map(index -> new AbstractMap.SimpleEntry<>(index, event));
    }

    private Set<IndexSet> indicesForEvent(Event event, Map<String, IndexSet> streamIndices) {
        return event.getStreams().stream()
                .map(streamId -> {
                    final IndexSet index = streamIndices.get(streamId);
                    if (index == null) {
                        LOG.warn("Couldn't find index set of stream <{}> for event <{}> (definition: {}/{})", streamId,
                                event.getId(), event.getEventDefinitionType(), event.getEventDefinitionId());
                    }
                    return index;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
