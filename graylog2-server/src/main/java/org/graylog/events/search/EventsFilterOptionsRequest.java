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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;

/**
 * Request for the values available to filter the events table by.
 *
 * @param fields     the event fields to return available values for
 * @param query      optional query to narrow the events the values are collected from, e.g. to scope
 *                   the result to a single source stream
 * @param fieldQuery optional search text the returned values must contain (case-insensitive)
 * @param timerange  the time range to collect values from, defaulting to the last 30 days
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventsFilterOptionsRequest(@JsonProperty("fields") List<String> fields,
                                         @JsonProperty("query") String query,
                                         @JsonProperty("field_query") String fieldQuery,
                                         @JsonProperty("timerange") TimeRange timerange) {

    private static final int DEFAULT_RANGE_SECONDS = 30 * 24 * 60 * 60;

    // Defensive cap: field values are short (tags max out at 128 chars), so longer search text can
    // never match and would only bloat the pattern sent to the search backend.
    private static final int MAX_FIELD_QUERY_LENGTH = 256;

    public EventsFilterOptionsRequest {
        fields = fields == null ? List.of() : fields;
        query = query == null ? "" : query;
        fieldQuery = fieldQuery == null ? "" : fieldQuery.substring(0, Math.min(fieldQuery.length(), MAX_FIELD_QUERY_LENGTH));
        timerange = timerange == null ? defaultTimerange() : timerange;
    }

    public static EventsFilterOptionsRequest empty() {
        return new EventsFilterOptionsRequest(List.of(), "", "", null);
    }

    private static TimeRange defaultTimerange() {
        try {
            return RelativeRange.create(DEFAULT_RANGE_SECONDS);
        } catch (InvalidRangeParametersException e) {
            throw new IllegalStateException("Unable to create the default filter options time range", e);
        }
    }
}
