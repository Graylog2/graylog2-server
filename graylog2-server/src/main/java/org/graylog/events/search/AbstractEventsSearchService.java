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
package org.graylog.events.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.indexer.template.IndexMapping;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

abstract class AbstractEventsSearchService {
    private final DBEventDefinitionService eventDefinitionService;
    private final StreamService streamService;
    private final ObjectMapper objectMapper;

    protected AbstractEventsSearchService(DBEventDefinitionService eventDefinitionService,
                                          StreamService streamService,
                                          ObjectMapper objectMapper) {
        this.eventDefinitionService = eventDefinitionService;
        this.streamService = streamService;
        this.objectMapper = objectMapper;
    }

    protected List<EventsSearchResult.Event> toEvents(MoreSearch.Result result,
                                                      ImmutableSet.Builder<String> eventDefinitionIdsBuilder,
                                                      ImmutableSet.Builder<String> streamIdsBuilder) {
        return result.results().stream()
                .map(resultMsg -> {
                    final EventDto eventDto = objectMapper.convertValue(resultMsg.getMessage().getFields(), EventDto.class);

                    eventDefinitionIdsBuilder.add((String) resultMsg.getMessage().getField(EventDto.FIELD_EVENT_DEFINITION_ID));
                    streamIdsBuilder.addAll(resultMsg.getMessage().getStreamIds());

                    return EventsSearchResult.Event.create(eventDto, resultMsg.getIndex(), IndexMapping.TYPE_MESSAGE);
                }).collect(Collectors.toList());
    }

    protected EventsSearchResult assembleResult(EventsSearchParameters parameters,
                                                MoreSearch.Result result,
                                                List<EventsSearchResult.Event> events,
                                                EventsSearchResult.Context context) {
        return EventsSearchResult.builder()
                .parameters(parameters)
                .totalEvents(result.resultsCount())
                .duration(result.duration())
                .events(events)
                .usedIndices(result.usedIndexNames())
                .context(context)
                .build();
    }

    protected Set<String> validateSystemEventStreams(Set<String> streamIds) {
        final Set<String> defaults = defaultEventStreams();
        if (streamIds == null || streamIds.isEmpty()) {
            return defaults;
        }
        if (!defaults.containsAll(streamIds)) {
            throw new IllegalArgumentException("Unsupported system event streams: " + streamIds);
        }
        return streamIds;
    }

    Map<String, EventsSearchResult.ContextEntity> lookupStreams(Set<String> streams, final Subject subject) {
        final var allowedStreams = streams.stream()
                .filter(streamId -> subject.isPermitted(String.join(":", RestPermissions.STREAMS_READ, streamId)))
                .collect(Collectors.toSet());

        return lookupStreams(allowedStreams);
    }

    protected Map<String, EventsSearchResult.ContextEntity> lookupStreams(Set<String> streams) {
        return streamService.loadByIds(streams)
                .stream()
                .collect(Collectors.toMap(Stream::getId,
                        s -> EventsSearchResult.ContextEntity.create(s.getId(), s.getTitle(), s.getDescription())));
    }

    protected Map<String, EventsSearchResult.ContextEntity> lookupEventDefinitions(Set<String> eventDefinitions,
                                                                                   final Subject subject) {
        final var allowedEventDefinitions = eventDefinitions.stream()
                .filter(eventDefinitionId -> subject.isPermitted(String.join(":", RestPermissions.EVENT_DEFINITIONS_READ, eventDefinitionId)))
                .collect(Collectors.toSet());

        return lookupEventDefinitions(allowedEventDefinitions);
    }

    protected Map<String, EventsSearchResult.ContextEntity> lookupEventDefinitions(Set<String> eventDefinitions) {
        return eventDefinitionService.getByIds(eventDefinitions)
                .stream()
                .collect(Collectors.toMap(EventDefinitionDto::id,
                        d -> EventsSearchResult.ContextEntity.create(d.id(), d.title(), d.description(), d.remediationSteps())));
    }

    protected Set<String> defaultEventStreams() {
        return Set.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID);
    }
}
