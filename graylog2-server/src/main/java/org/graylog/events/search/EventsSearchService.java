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
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;

public class EventsSearchService extends AbstractEventsSearchService {
    private final MoreSearch moreSearch;
    private final StreamService streamService;

    @Inject
    public EventsSearchService(MoreSearch moreSearch,
                               StreamService streamService,
                               DBEventDefinitionService eventDefinitionService,
                               ObjectMapper objectMapper) {
        super(eventDefinitionService, streamService, objectMapper);
        this.moreSearch = moreSearch;
        this.streamService = streamService;
    }

    private String buildFilter(EventsSearchParameters parameters) {
        return new EventsFilterBuilder(parameters).build();
    }

    private Set<String> allowedEventStreams(Subject subject) {
        final var eventStreams = defaultEventStreams();
        if (subject.isPermitted(RestPermissions.STREAMS_READ)) {
            return eventStreams;
        }

        return eventStreams.stream()
                .filter(streamId -> subject.isPermitted(String.join(":", RestPermissions.STREAMS_READ, streamId)) || streamId.equals(DEFAULT_EVENTS_STREAM_ID))
                .collect(Collectors.toSet());
    }

    public EventsSearchResult search(EventsSearchParameters parameters, Subject subject) {
        final var eventStreams = allowedEventStreams(subject);
        if (eventStreams.isEmpty()) {
            return EventsSearchResult.empty();
        }

        final var filter = buildFilter(parameters);

        final MoreSearch.Result result = moreSearch.eventSearch(parameters, filter, eventStreams, forbiddenSourceStreams(subject));

        return buildResultForSubject(parameters, result, subject);
    }

    public EventsHistogramResult histogram(EventsSearchParameters parameters, Subject subject, ZoneId timeZone) {
        final var eventStreams = allowedEventStreams(subject);
        if (eventStreams.isEmpty()) {
            return EventsHistogramResult.fromResult(MoreSearch.Histogram.empty());
        }

        final var filter = buildFilter(parameters);
        final var result = moreSearch.histogram(parameters, filter, eventStreams, forbiddenSourceStreams(subject), timeZone);

        return EventsHistogramResult.fromResult(result);
    }

    public EventsSearchResult searchByIds(Collection<String> eventIds, Subject subject) {
        final var query = eventIds.stream()
                .map(eventId -> EventDto.FIELD_ID + ":" + eventId)
                .collect(Collectors.joining(" OR "));
        final EventsSearchParameters parameters = EventsSearchParameters.builder()
                .page(1)
                .perPage(eventIds.size())
                .timerange(RelativeRange.allTime())
                .query(query)
                .filter(EventsSearchFilter.empty())
                .sortBy(Message.FIELD_TIMESTAMP)
                .sortDirection(EventsSearchParameters.SortDirection.DESC)
                .build();

        return search(parameters, subject);
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

        try (var stream = streamService.streamAllIds()) {
            return stream
                    // Select all streams the user is NOT permitted to access
                    .filter(streamId -> !subject.isPermitted(String.join(":", RestPermissions.STREAMS_READ, streamId)))
                    .collect(Collectors.toSet());
        }
    }

    private EventsSearchResult buildResultForSubject(EventsSearchParameters parameters,
                                                     MoreSearch.Result result,
                                                     Subject subject) {
        final ImmutableSet.Builder<String> eventDefinitionIdsBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> streamIdsBuilder = ImmutableSet.builder();
        final List<EventsSearchResult.Event> events = toEvents(result, eventDefinitionIdsBuilder, streamIdsBuilder);
        final EventsSearchResult.Context context = EventsSearchResult.Context.create(
                lookupEventDefinitions(eventDefinitionIdsBuilder.build(), subject),
                lookupStreams(streamIdsBuilder.build(), subject));

        return assembleResult(parameters, result, events, context);
    }
}
