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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.Parameter;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ExecutionState.Builder.class)
public abstract class ExecutionState {
    @JsonProperty
    public abstract ImmutableMap<String, Parameter.Binding> parameterBindings();

    @JsonProperty
    public abstract ImmutableMap<String, ExecutionStateGlobalOverride> queries();

    @JsonProperty
    public abstract ExecutionStateGlobalOverride globalOverride();

    @JsonProperty
    public abstract ImmutableMap<String, Object> additionalParameters();

    public static ExecutionState empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new AutoValue_ExecutionState.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return ExecutionState.builder();
        }

        @JsonProperty("global_override")
        public abstract Builder setGlobalOverride(ExecutionStateGlobalOverride globalOverride);

        public abstract ExecutionStateGlobalOverride.Builder globalOverrideBuilder();

        @JsonProperty
        public abstract ImmutableMap.Builder<String, ExecutionStateGlobalOverride> queriesBuilder();

        public abstract ImmutableMap.Builder<String, Parameter.Binding> parameterBindingsBuilder();

        @JsonProperty("parameter_bindings")
        public Builder withParameterBindings(Map<String, Parameter.Binding> values) {
            values.forEach((s, o) -> parameterBindingsBuilder().put(s, o));
            return this;
        }


        public abstract ImmutableMap.Builder<String, Object> additionalParametersBuilder();

        @JsonProperty("additional_parameters")
        public Builder withAdditionalParameters(Map<String, Object> values) {
            values.forEach((s, o) -> additionalParametersBuilder().put(s, o));
            return this;
        }

        @JsonAnySetter
        public Builder addAdditionalParameter(String key, Object value) {
            additionalParametersBuilder().put(key, value);
            return this;
        }

        public abstract ExecutionState build();
    }
}
