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
import org.apache.lucene.search.TermRangeQuery;
import org.apache.shiro.subject.Subject;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

public class EventsSearchService {
    private static final Collector<CharSequence, ?, String> joiningQueriesWithAND = Collectors.joining(" AND ");
    private static final Collector<CharSequence, ?, String> joiningQueriesWithOR = Collectors.joining(" OR ");
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
        final var filterBuilder = new ArrayList<String>();
        // Make sure we only filter for actual events and ignore anything else that might be in the event
        // indices. (fixes an issue when users store non-event messages in event indices)
        filterBuilder.add("_exists_:" + EventDto.FIELD_EVENT_DEFINITION_ID);

        if (!parameters.filter().eventDefinitions().isEmpty()) {
            final String eventDefinitionFilter = parameters.filter().eventDefinitions().stream()
                    .map(this::eventDefinitionFilter)
                    .collect(joiningQueriesWithOR);

            filterBuilder.add(eventDefinitionFilter);
        }

        if (!parameters.filter().priority().isEmpty()) {
            filterBuilder.add(parameters.filter().priority().stream()
                    .map(this::mapPriority)
                    .map(priority -> EventDto.FIELD_PRIORITY + ":" + priority)
                    .collect(joiningQueriesWithOR));
        }

        parameters.filter().aggregationTimerange()
                .filter(range -> !range.isAllMessages())
                .ifPresent(aggregationTimerange -> filterBuilder.add(createTimeRangeFilter(aggregationTimerange)));

        if (!parameters.filter().key().isEmpty()) {
            filterBuilder.add(parameters.filter().key().stream()
                    .map(keyFilter -> EventDto.FIELD_KEY_TUPLE + ":" + quote(keyFilter))
                    .collect(joiningQueriesWithOR));
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

        final String filter = filterBuilder.stream()
                .map(query -> "(" + query + ")")
                .collect(joiningQueriesWithAND);
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

    private String createTimeRangeFilter(TimeRange aggregationTimerange) {
        final var formattedFrom = aggregationTimerange.getFrom().toString(ES_DATE_FORMAT_FORMATTER);
        final var formattedTo = aggregationTimerange.getTo().toString(ES_DATE_FORMAT_FORMATTER);
        return or(
                group(
                        or(
                                TermRangeQuery.newStringRange(
                                        EventDto.FIELD_TIMERANGE_START,
                                        quote(formattedFrom),
                                        quote(formattedTo),
                                        true,
                                        true).toString()
                                ,
                                TermRangeQuery.newStringRange(
                                        EventDto.FIELD_TIMERANGE_END,
                                        quote(formattedFrom),
                                        quote(formattedTo),
                                        true,
                                        true).toString()
                        )
                ),
                group(
                        and(
                                TermRangeQuery.newStringRange(
                                        EventDto.FIELD_TIMERANGE_START,
                                        quote("1970-01-01 00:00:00.000"),
                                        quote(formattedFrom),
                                        true,
                                        true).toString()
                                ,
                                TermRangeQuery.newStringRange(
                                        EventDto.FIELD_TIMERANGE_END,
                                        quote(formattedTo),
                                        quote("2038-01-01 00:00:00.000"),
                                        true,
                                        true).toString()
                        )
                )
        );
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    private String group(String s) {
        return "(" + s + ")";
    }

    private String or(String... queries) {
        return Arrays.stream(queries).collect(joiningQueriesWithOR);
    }

    private String and(String... queries) {
        return Arrays.stream(queries).collect(joiningQueriesWithAND);
    }

    private long mapPriority(String priorityFilter) {
        return switch (priorityFilter) {
            case "high", "3" -> 3;
            case "normal", "2" -> 2;
            case "low", "1" -> 1;
            default -> throw new IllegalStateException("Invalid priority: " + priorityFilter);
        };
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
        return streamService.loadByIds(streams)
                .stream()
                .collect(Collectors.toMap(Persisted::getId, s -> EventsSearchResult.ContextEntity.create(s.getId(), s.getTitle(), s.getDescription())));
    }

    private Map<String, EventsSearchResult.ContextEntity> lookupEventDefinitions(Set<String> eventDefinitions) {
        return eventDefinitionService.getByIds(eventDefinitions)
                .stream()
                .collect(Collectors.toMap(EventDefinitionDto::id,
                        d -> EventsSearchResult.ContextEntity.create(d.id(), d.title(), d.description(), d.remediationSteps())));
    }
}
