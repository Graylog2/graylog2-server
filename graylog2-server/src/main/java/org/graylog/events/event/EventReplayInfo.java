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
package org.graylog.events.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Stores the information necessary to recreate a query that triggered a search-based event. Allows those events to
 * maintain the information even if the corresponding event definition gets modified or deleted.
 */
@AutoValue
@JsonDeserialize(builder = EventReplayInfo.Builder.class)
public abstract class EventReplayInfo {
    public static final String FIELD_TIMERANGE_START = "timerange_start";
    public static final String FIELD_TIMERANGE_END = "timerange_end";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_STREAMS = "streams";
    public static final String FIELD_FILTERS = "filters";

    @JsonProperty(FIELD_TIMERANGE_START)
    public abstract DateTime timerangeStart();

    @JsonProperty(FIELD_TIMERANGE_END)
    public abstract DateTime timerangeEnd();

    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_FILTERS)
    public abstract List<UsedSearchFilter> filters();

    public static Builder builder() {
        return new AutoValue_EventReplayInfo.Builder()
                .filters(Collections.emptyList());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_TIMERANGE_START)
        public abstract Builder timerangeStart(DateTime timeRangeStart);

        @JsonProperty(FIELD_TIMERANGE_END)
        public abstract Builder timerangeEnd(DateTime timeRangeEnd);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_FILTERS)
        public abstract Builder filters(List<UsedSearchFilter> filters);

        public abstract EventReplayInfo build();

        @JsonCreator
        public static Builder create() {
            return builder();
        }
    }
}
