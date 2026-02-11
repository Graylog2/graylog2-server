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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EventsSlicesRequest.Builder.class)
public abstract class EventsSlicesRequest {
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_SLICE_COLUMN = "slice_column";
    private static final String FIELD_INCLUDE_ALL = "include_all";

    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_FILTER)
    public abstract EventsSearchFilter filter();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract TimeRange timerange();

    @JsonProperty(FIELD_SLICE_COLUMN)
    public abstract String sliceColumn();

    @JsonProperty(FIELD_INCLUDE_ALL)
    public abstract boolean includeAll();

    public static Builder builder() {
        return Builder.create();
    }

    public static EventsSlicesRequest empty() {
        return builder().build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            RelativeRange timerange = null;
            try {
                timerange = RelativeRange.create(3600);
            } catch (InvalidRangeParametersException e) {
                // Should not happen
            }
            return new AutoValue_EventsSlicesRequest.Builder()
                    .query("")
                    .filter(EventsSearchFilter.empty())
                    .timerange(timerange)
                    .includeAll(false);
        }

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(EventsSearchFilter filter);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty(FIELD_SLICE_COLUMN)
        public abstract Builder sliceColumn(String sliceColumn);

        @JsonProperty(FIELD_INCLUDE_ALL)
        public abstract Builder includeAll(boolean includeAll);

        public abstract EventsSlicesRequest build();
    }
}
