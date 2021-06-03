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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = FieldTypesForStreamsRequest.Builder.class)
public abstract class FieldTypesForStreamsRequest {
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_TIMERANGE = "timerange";

    @JsonProperty(FIELD_STREAMS)
    public abstract Optional<Set<String>> streams();

    @JsonProperty(FIELD_TIMERANGE)
    public abstract Optional<TimeRange> timerange();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(@Nullable Set<String> streams);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract Builder timerange(@Nullable TimeRange timerange);

        public abstract FieldTypesForStreamsRequest build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_FieldTypesForStreamsRequest.Builder();
        }
    }
}
