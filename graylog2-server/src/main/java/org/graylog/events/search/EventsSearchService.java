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
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

public class EventsSearchService {
    private final MoreSearch moreSearch;
    private final StreamService streamService;
    private final DBEventDefinitionService eventDefinitionService;
    private final ObjectMapper objectMapper;

    @Inject
    public EventsSearchService(MoreSearch moreSearch,
                               StreamService streamService,
                               DBEventDefinitionService eventDefinitionService,
                               ObjectMapper objectMapper) {
        this.moreSearch = moreSearch;
        this.streamService = streamService;
        this.eventDefinitionService = eventDefinitionService;
        this.objectMapper = objectMapper;
    }

    private String eventDefinitionFilter(String id) {
        return String.format(Locale.ROOT, "%s:%s", EventDto.FIELD_EVENT_DEFINITION_ID, id);
    }

    public EventsSearchResult search(EventsSearchParameters parameters, Subject subject) {
        final ImmutableSet.Builder<String> filterBuilder = ImmutableSet.<String>builder()
                // Make sure we only filter for actual events and ignore anything else that might be in the event
                // indices. (fixes an issue when users store non-event messages in event indices)
                .add("_exists_:" + EventDto.FIELD_EVENT_DEFINITION_ID);

        if (!parameters.filter().eventDefinitions().isEmpty()) {
            final String eventDefinitionFilter = parameters.filter().eventDefinitions().stream()
                    .map(this::eventDefinitionFilter)
                    .collect(Collectors.joining(" OR "));

            filterBuilder.addAll(Collections.singleton("(" + eventDefinitionFilter + ")"));
        }

        switch (parameters.filter().alerts()) {
            case INCLUDE:
                // Nothing to do
                break;
            case EXCLUDE:
                filterBuilder.add("NOT alert:true");
                break;
            case ONLY:
                filterBuilder.add("alert:true");
                break;
        }

        final String filter = String.join(" AND ", filterBuilder.build());
        final ImmutableSet<String> eventStreams = ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID);
        final MoreSearch.Result result = moreSearch.eventSearch(parameters, filter, eventStreams, forbiddenSourceStreams(subject));

        final ImmutableSet.Builder<String> eventDefinitionIdsBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> streamIdsBuilder = ImmutableSet.builder();

        final List<EventsSearchResult.Event> events = result.results().stream()
                .map(resultMsg -> {
                    final EventDto eventDto = objectMapper.convertValue(resultMsg.getMessage().getFields(), EventDto.class);

                    eventDefinitionIdsBuilder.add((String) resultMsg.getMessage().getField(EventDto.FIELD_EVENT_DEFINITION_ID));
                    streamIdsBuilder.addAll(resultMsg.getMessage().getStreamIds());

                    return EventsSearchResult.Event.create(eventDto, resultMsg.getIndex(), IndexMapping.TYPE_MESSAGE);
                }).collect(Collectors.toList());

        final EventsSearchResult.Context context = EventsSearchResult.Context.create(
                lookupEventDefinitions(eventDefinitionIdsBuilder.build()),
                lookupStreams(streamIdsBuilder.build())
        );

        return EventsSearchResult.builder()
                .parameters(parameters)
                .totalEvents(result.resultsCount())
                .duration(result.duration())
                .events(events)
                .usedIndices(result.usedIndexNames())
                .context(context)
                .build();
    }

    // TODO: Loading all streams for a user is not very efficient. Not sure if we can find an alternative that is
    //       more efficient. Doing a separate ES query to get all source streams that would be in the result is
    //       most probably not more efficient.
    private Set<String> forbiddenSourceStreams(Subject subject) {
        // Users with the generic streams:read permission can read all streams so we don't need to check every single
        // stream here and can take a short cut.
        if (subject.isPermitted(RestPermissions.STREAMS_READ)) {
            return Collections.emptySet();
        }

        return streamService.loadAll().stream()
                .map(Persisted::getId)
                // Select all streams the user is NOT permitted to access
                .filter(streamId -> !subject.isPermitted(String.join(":", RestPermissions.STREAMS_READ, streamId)))
                .collect(Collectors.toSet());
    }

    private Map<String, EventsSearchResult.ContextEntity> lookupStreams(Set<String> streams) {
        return streams.stream()
                .map(streamId -> {
                    try {
                        return streamService.load(streamId);
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Persisted::getId, s -> EventsSearchResult.ContextEntity.create(s.getId(), s.getTitle(), s.getDescription())));
    }

    private Map<String, EventsSearchResult.ContextEntity> lookupEventDefinitions(Set<String> eventDefinitions) {
        return eventDefinitions.stream()
                .map(eventDefinitionService::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(EventDefinitionDto::id, d -> EventsSearchResult.ContextEntity.create(d.id(), d.title(), d.description())));
    }
}
