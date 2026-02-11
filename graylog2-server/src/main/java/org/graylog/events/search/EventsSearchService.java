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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.entities.Slices;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.events.event.EventDto.FIELD_ALERT;
import static org.graylog.events.event.EventDto.FIELD_PRIORITY;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;

public class EventsSearchService extends AbstractEventsSearchService {
    private final MoreSearch moreSearch;
    private final StreamService streamService;
    private final ScriptingApiService scriptingApiService;

    @Inject
    public EventsSearchService(MoreSearch moreSearch,
                               StreamService streamService,
                               DBEventDefinitionService eventDefinitionService,
                               final ScriptingApiService scriptingApiService,
                               ObjectMapper objectMapper) {
        super(eventDefinitionService, streamService, objectMapper);
        this.moreSearch = moreSearch;
        this.streamService = streamService;
        this.scriptingApiService = scriptingApiService;
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

    public Slices slices(final EventsSlicesRequest request, final Subject subject, final SearchUser searchUser) {
        // we cover two use cases, if you only want the slices from the resultset that will also be shown in the entity table, we re-use query and timerange for that. Otherwise, we query "all"
        final var query = request.includeAll() ? "" : request.query();
        final var timeRange = request.includeAll() ? RelativeRange.allTime() : request.timerange();

        return slices(query, timeRange, subject, searchUser, request.sliceColumn(), request.includeAll());
    }

    /**
     * finding the overall count for one column for MongoDB based queries so we can calculate the "empty" case
     */
    public Optional<Slice> count(String query, TimeRange timeRange, Subject subject, SearchUser searchUser, final String slicingColumn) {
        try {
            return scriptingApiService.executeAggregation(
                            new AggregationRequestSpec(query, allowedEventStreams(subject), Set.of(), timeRange, List.of(), List.of(new Metric("count", null))),
                            searchUser
                    )
                    .datarows()
                    .stream()
                    .map(r -> new Slice(r.getFirst().toString(), r.getFirst().toString(), Integer.valueOf(r.getLast().toString())))
                    .findFirst();
        } catch (QueryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * In the Open Source part, we only map priority and type, both of which are augmented in the FE regarding the title.
     * So we only need a simple mapping function
     * @param slicingColumn
     * @param result
     * @return Slice
     */
    Slice mapAggregationResultsToSlice(final String slicingColumn, final List<Object> result) {
        return new Slice(result.getFirst().toString(), null, Integer.valueOf(result.getLast().toString()));
    }

    // the alert can either be true or false
    List<Slice> handleAlertColumn(final List<Slice> slices) {
        if(slices.size() == 2) {
            return slices;
        }

        final var TRUE = new Slice( "true", null, 0);
        final var FALSE = new Slice( "false", null, 0);

        if(slices.isEmpty()) {
            return List.of(TRUE, FALSE);
        }

        if(slices.getFirst().value().equals("true")) {
            return List.of(slices.getFirst(), FALSE);
        } else {
            return List.of(TRUE, slices.getFirst());
        }
    }

    // priority can be 1 (low) to 4 (critical), see EventDefinitionPriority.ts
    List<Slice> handlePriorityColumn(final List<Slice> slices) {
        if(slices.size() == 4) {
            return slices;
        }

        final var LOW = new Slice( "1", null, 0);
        final var MEDIUM = new Slice( "2", null, 0);
        final var HIGH = new Slice( "3", null, 0);
        final var CRITICAL = new Slice( "4", null, 0);

        if(slices.isEmpty()) {
            return List.of(LOW, MEDIUM, HIGH, CRITICAL);
        }

        List<Slice> fixedList = new ArrayList<>();
        fixedList.add(slices.stream().filter(s -> s.value().equals(LOW.value())).findAny().orElse(LOW));
        fixedList.add(slices.stream().filter(s -> s.value().equals(MEDIUM.value())).findAny().orElse(MEDIUM));
        fixedList.add(slices.stream().filter(s -> s.value().equals(HIGH.value())).findAny().orElse(HIGH));
        fixedList.add(slices.stream().filter(s -> s.value().equals(CRITICAL.value())).findAny().orElse(CRITICAL));

        return fixedList;
    }

    private List<Slice> addMissingOptions(final List<Slice> slices, final String slicingColumn) {
        return switch (slicingColumn) {
            case FIELD_ALERT -> handleAlertColumn(slices);
            case FIELD_PRIORITY -> handlePriorityColumn(slices);
            default -> slices;
        };
    }

    /**
     * finding all slices for a particular column in the Indexer
     */
    public Slices slices(String query, TimeRange timeRange, Subject subject, SearchUser searchUser, final String slicingColumn, final boolean includeAll) {
        try {
            final var slices = scriptingApiService.executeAggregation(
                            new AggregationRequestSpec(query, allowedEventStreams(subject), Set.of(), timeRange, List.of(new Grouping(slicingColumn, Integer.MAX_VALUE)), List.of(new Metric("count", slicingColumn))),
                            searchUser
                    )
                    .datarows()
                    .stream()
                    .map(r -> mapAggregationResultsToSlice(slicingColumn, r))
                    .toList();

            final var filtered = includeAll
                    ? addMissingOptions(slices, slicingColumn)
                    : slices.stream().filter(s -> !"(Empty Value)".equals(s.value())).toList();

            return new Slices(filtered);
        } catch (QueryFailedException e) {
            throw new RuntimeException(e);
        }
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
