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
import org.graylog2.rest.resources.entities.SlicesRequest;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EventsSlicesRequest.Builder.class)
public abstract class EventsSlicesRequest implements SlicesRequest<EventsSearchParameters> {
    private static final String FIELD_PARAMETERS = "parameters";
    private static final String FIELD_SLICE_COLUMN = "slice_column";
    private static final String FIELD_SORT_BY = "sort_by";
    private static final String FIELD_SORT_DIRECTION = "sort_direction";
    private static final String FIELD_INCLUDE_ALL = "include_all";

    @JsonProperty(FIELD_PARAMETERS)
    @Override
    public abstract EventsSearchParameters parameters();

    @JsonProperty(FIELD_SLICE_COLUMN)
    @Override
    public abstract String sliceColumn();

    @JsonProperty(FIELD_SORT_BY)
    @Override
    public abstract String sortBy();

    @JsonProperty(FIELD_SORT_DIRECTION)
    @Override
    public abstract SortDirection sortDirection();

    @JsonProperty(FIELD_INCLUDE_ALL)
    @Override
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
            return new AutoValue_EventsSlicesRequest.Builder()
                    .sliceColumn("type")
                    .parameters(EventsSearchParameters.empty())
                    .includeAll(false)
                    .sortBy("title")
                    .sortDirection(SortDirection.ASC);
        }

        @JsonProperty(FIELD_PARAMETERS)
        public abstract Builder parameters(EventsSearchParameters parameters);

        @JsonProperty(FIELD_SLICE_COLUMN)
        public abstract Builder sliceColumn(String sliceColumn);

        @JsonProperty(FIELD_SORT_BY)
        public abstract Builder sortBy(String sortBy);

        @JsonProperty(FIELD_SORT_DIRECTION)
        public abstract Builder sortDirection(SortDirection sortDirection);

        @JsonProperty(FIELD_INCLUDE_ALL)
        public abstract Builder includeAll(boolean includeAll);

        public abstract EventsSlicesRequest build();
    }
}
