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
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EventsSearchParameters.Builder.class)
public abstract class EventsSearchParameters {
    private static final String FIELD_PAGE = "page";
    private static final String FIELD_PER_PAGE = "per_page";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_SORT_BY = "sort_by";
    private static final String FIELD_SORT_DIRECTION = "sort_direction";

    public enum SortDirection {
        @JsonProperty("asc")
        ASC,
        @JsonProperty("desc")
        DESC
    }

    @JsonProperty(FIELD_PAGE)
    public abstract int page();

    @JsonProperty(FIELD_PER_PAGE)
    public abstract int perPage();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract TimeRange timerange();

    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_FILTER)
    public abstract EventsSearchFilter filter();

    @JsonProperty(FIELD_SORT_BY)
    public abstract String sortBy();

    @JsonProperty(FIELD_SORT_DIRECTION)
    public abstract SortDirection sortDirection();

    public static Builder builder() {
        return Builder.create();
    }

    public static EventsSearchParameters empty() {
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
            return new AutoValue_EventsSearchParameters.Builder()
                    .page(1)
                    .perPage(10)
                    .timerange(timerange)
                    .query("")
                    .filter(EventsSearchFilter.empty())
                    .sortBy(Message.FIELD_TIMESTAMP)
                    .sortDirection(SortDirection.DESC);
        }

        @JsonProperty(FIELD_PAGE)
        public abstract Builder page(int page);

        @JsonProperty(FIELD_PER_PAGE)
        public abstract Builder perPage(int perPage);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(TimeRange timerange);

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_FILTER)
        public abstract Builder filter(EventsSearchFilter filter);

        @JsonProperty(FIELD_SORT_BY)
        public abstract Builder sortBy(String sortBy);

        @JsonProperty(FIELD_SORT_DIRECTION)
        public abstract Builder sortDirection(SortDirection sortDirection);

        public abstract EventsSearchParameters build();
    }
}
