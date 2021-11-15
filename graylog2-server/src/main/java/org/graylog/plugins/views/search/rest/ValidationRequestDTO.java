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
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.ValidationRequest;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ValidationRequestDTO.Builder.class)
public abstract class ValidationRequestDTO {

    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_TIMERANGE = "timerange";

    @JsonProperty
    public abstract BackendQuery query();

    @Nullable
    @JsonProperty(FIELD_TIMERANGE)
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty
    public abstract ImmutableMap<String,  Parameter.Binding> parameterBindings();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract ValidationRequestDTO.Builder query(BackendQuery query);


        @JsonProperty(FIELD_STREAMS)
        public abstract ValidationRequestDTO.Builder streams(@Nullable Set<String> streams);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract ValidationRequestDTO.Builder timerange(@Nullable TimeRange timerange);

        public abstract ImmutableMap.Builder<String,  Parameter.Binding> parameterBindingsBuilder();

        @JsonProperty("parameter_bindings")
        public Builder withParameterBindings(Map<String,  Parameter.Binding> values) {
            values.forEach((s, o) -> parameterBindingsBuilder().put(s, o));
            return this;
        }


        public abstract ValidationRequestDTO build();

        @JsonCreator
        public static ValidationRequestDTO.Builder builder() {
            return new AutoValue_ValidationRequestDTO.Builder();
        }
    }
}
