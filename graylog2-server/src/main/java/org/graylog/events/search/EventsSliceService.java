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
import com.google.common.collect.Streams;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.entities.Slices;
import org.graylog2.streams.StreamService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.events.event.EventDto.FIELD_ALERT;
import static org.graylog.events.event.EventDto.FIELD_PRIORITY;

public class EventsSliceService extends AbstractEventsSearchService {
    private final ScriptingApiService scriptingApiService;

    @Inject
    public EventsSliceService(final ScriptingApiService scriptingApiService,
                              final StreamService streamService,
                              final DBEventDefinitionService eventDefinitionService,
                              final ObjectMapper objectMapper) {
        super(eventDefinitionService, streamService, objectMapper);
        this.scriptingApiService = scriptingApiService;
    }

    protected ScriptingApiService getScriptingApiService() {
        return scriptingApiService;
    }

    /**
     * returns the slices for the slice-by functionality for the Alerts/Events table. Used in the core Events/Alerts table
     */
    public Slices slices(final EventsSlicesRequest request, final Subject subject, final SearchUser searchUser) {
        // we cover two use cases by the include_all flag: if you only want the slices calculated from the resultset that will also be shown in the entity table, we re-use query and timerange for that. Otherwise, we query the table for "all" possible slices
        final var query = request.query();
        final var timeRange = request.timerange();
        final var filter = buildFilter(EventsSearchParameters.builder().query(query).timerange(timeRange).filter(request.filter()).build());

        final var queryString = filter.isEmpty() ? query : query.isEmpty() ? filter : query + " AND " + filter;

        final var allSlicesInTimeRange = this.slices("", timeRange, subject, searchUser, request.sliceColumn());
        final var filteredSlices = this.slices(queryString, timeRange, subject, searchUser, request.sliceColumn());
        final var slices = filteredSlicesWithMergedInEmptySlicesFrom(filteredSlices, allSlicesInTimeRange, getTypeBySliceColumn(request.sliceColumn()));

        return new Slices(addMissingOptions(slices, request.sliceColumn()));
    }

    protected static List<Slice> filteredSlicesWithMergedInEmptySlicesFrom(final List<Slice> filteredSlices, final List<Slice> allSlicesInTimeRange, final String type) {
        final var existingSlices = filteredSlices.stream().map(Slice::value).collect(Collectors.toSet());
        final var emptySlices = allSlicesInTimeRange.stream().filter(slice -> !existingSlices.contains(slice.value())).map(s -> new Slice(s.value(), s.title(), type, 0));
        return Streams.concat(filteredSlices.stream(), emptySlices).toList();
    }

    /**
     * Used inside this service for the core Events/Alerts table but also is a provided method, re-used from the enterprise/security part of Graylog
     */
    protected List<Slice> slices(String query, TimeRange timeRange, Subject subject, SearchUser searchUser, final String slicingColumn) {
        try {
            return scriptingApiService.executeAggregation(
                            new AggregationRequestSpec(query, allowedEventStreams(subject), Set.of(), timeRange, List.of(new Grouping(slicingColumn, Integer.MAX_VALUE)), List.of(new Metric("count", slicingColumn))),
                            searchUser
                    )
                    .datarows()
                    .stream()
                    .map(r -> mapAggregationResultsToSlice(slicingColumn, r))
                    .toList();
        } catch (QueryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * finding the overall count for one column for MongoDB based queries so we can calculate the "empty" case
     */
    public Optional<Slice> count(String query, TimeRange timeRange, Subject subject, SearchUser searchUser, final String type) {
        try {
            return scriptingApiService.executeAggregation(
                            new AggregationRequestSpec(query, allowedEventStreams(subject), Set.of(), timeRange, List.of(), List.of(new Metric("count", null))),
                            searchUser
                    )
                    .datarows()
                    .stream()
                    .map(r -> new Slice(r.getFirst().toString(), r.getFirst().toString(), type, Integer.valueOf(r.getLast().toString())))
                    .findFirst();
        } catch (QueryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getTypeBySliceColumn(final String column) {
        return switch (column) {
            case FIELD_PRIORITY -> FIELD_PRIORITY;
            case FIELD_ALERT ->  FIELD_ALERT;
            default -> null;
        };
    }

    /**
     * In the Open Source part, we only map priority and type, both of which are augmented in the FE regarding the title.
     * So we only need a simple mapping function here.
     */
    protected Slice mapAggregationResultsToSlice(final String slicingColumn, final List<Object> result) {
        return new Slice(result.getFirst().toString(), null, getTypeBySliceColumn(slicingColumn), Integer.valueOf(result.getLast().toString()));
    }

    // the alert can either be true or false
    List<Slice> handleAlertColumn(final List<Slice> slices) {
        final var type = getTypeBySliceColumn(FIELD_ALERT);

        if (slices.size() == 2) {
            return slices;
        }

        final var TRUE = new Slice("true", null, type, 0);
        final var FALSE = new Slice("false", null, type, 0);

        if (slices.isEmpty()) {
            return List.of(TRUE, FALSE);
        }

        if (slices.getFirst().value().equals("true")) {
            return List.of(slices.getFirst(), FALSE);
        } else {
            return List.of(TRUE, slices.getFirst());
        }
    }

    // priority can be 0 (info) to 4 (critical), see EventDefinitionPriorityEnum.ts
    List<Slice> handlePriorityColumn(final List<Slice> slices) {
        final var type = getTypeBySliceColumn(FIELD_PRIORITY);

        if (slices.size() == 5) {
            return slices;
        }

        final var INFO = new Slice("0", null, type, 0);
        final var LOW = new Slice("1", null, type, 0);
        final var MEDIUM = new Slice("2", null, type, 0);
        final var HIGH = new Slice("3", null, type, 0);
        final var CRITICAL = new Slice("4", null, type, 0);

        if (slices.isEmpty()) {
            return List.of(INFO, LOW, MEDIUM, HIGH, CRITICAL);
        }

        List<Slice> fixedList = new ArrayList<>();
        fixedList.add(slices.stream().filter(s -> s.value().equals(INFO.value())).findAny().orElse(INFO));
        fixedList.add(slices.stream().filter(s -> s.value().equals(LOW.value())).findAny().orElse(LOW));
        fixedList.add(slices.stream().filter(s -> s.value().equals(MEDIUM.value())).findAny().orElse(MEDIUM));
        fixedList.add(slices.stream().filter(s -> s.value().equals(HIGH.value())).findAny().orElse(HIGH));
        fixedList.add(slices.stream().filter(s -> s.value().equals(CRITICAL.value())).findAny().orElse(CRITICAL));

        return fixedList;
    }

    // when slicing, add missing keys that don't exist in the data but you still want to show with cardinality 0
    protected List<Slice> addMissingOptions(final List<Slice> slices, final String slicingColumn) {
        return switch (slicingColumn) {
            case FIELD_ALERT -> handleAlertColumn(slices);
            case FIELD_PRIORITY -> handlePriorityColumn(slices);
            default -> slices;
        };
    }
}
