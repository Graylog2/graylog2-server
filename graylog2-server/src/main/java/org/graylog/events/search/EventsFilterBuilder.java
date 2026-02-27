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

import org.apache.lucene.search.TermRangeQuery;
import org.graylog.events.event.EventDto;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

public class EventsFilterBuilder {
    private static final Collector<CharSequence, ?, String> joiningQueriesWithAND = Collectors.joining(" AND ");
    private static final Collector<CharSequence, ?, String> joiningQueriesWithOR = Collectors.joining(" OR ");
    private final EventsSearchParameters parameters;

    public EventsFilterBuilder(EventsSearchParameters parameters) {
        this.parameters = parameters;
    }

    public String build() {
        final var filterBuilder = new ArrayList<String>();
        // Make sure we only filter for actual events and ignore anything else that might be in the event
        // indices. (fixes an issue when users store non-event messages in event indices)
        filterBuilder.add("_exists_:" + EventDto.FIELD_EVENT_DEFINITION_ID);

        if (!parameters.query().isBlank()) {
            filterBuilder.add(parameters.query());
        }

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

        if (!parameters.filter().id().isEmpty()) {
            filterBuilder.add(parameters.filter().id().stream()
                    .map(idFilter -> EventDto.FIELD_ID + ":" + quote(idFilter))
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

        return filterBuilder.stream()
                .map(query -> "(" + query + ")")
                .collect(joiningQueriesWithAND);
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

    private String eventDefinitionFilter(String id) {
        return String.format(Locale.ROOT, "%s:%s", EventDto.FIELD_EVENT_DEFINITION_ID, id);
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
            case "critical", "4" -> 4;
            case "high", "3" -> 3;
            case "normal", "2" -> 2;
            case "low", "1" -> 1;
            default -> throw new IllegalStateException("Invalid priority: " + priorityFilter);
        };
    }
}
