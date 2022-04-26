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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = ExecutionStateGlobalOverride.Builder.class)
public abstract class ExecutionStateGlobalOverride {
    @JsonProperty
    public abstract Optional<TimeRange> timerange();

    @JsonProperty
    public abstract Optional<BackendQuery> query();

    @JsonProperty
    public abstract Optional<Integer> limit();

    @JsonProperty
    public abstract Optional<Integer> offset();

    @JsonProperty
    public abstract ImmutableMap<String, SearchTypeExecutionState> searchTypes();

    @JsonProperty
    public abstract ImmutableSet<String> keepSearchTypes();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_ExecutionStateGlobalOverride.Builder();
    }

    public boolean hasValues() {
        return timerange().isPresent() ||
                query().isPresent() ||
                limit().isPresent() ||
                offset().isPresent() ||
                !searchTypes().isEmpty() ||
                !keepSearchTypes().isEmpty();
    }

    public static ExecutionStateGlobalOverride empty() {
        return builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return ExecutionStateGlobalOverride.builder();
        }

        @JsonProperty
        public abstract Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty
        public abstract Builder query(@Nullable BackendQuery query);

        @JsonProperty
        public abstract Builder limit(@Nullable Integer limit);

        @JsonProperty
        public abstract Builder offset(@Nullable Integer offset);

        @JsonProperty
        public abstract Builder keepSearchTypes(ImmutableSet<String> keepSearchTypes);

        @JsonProperty
        public abstract Builder searchTypes(ImmutableMap<String, SearchTypeExecutionState> searchTypes);

        public abstract ImmutableMap.Builder<String, SearchTypeExecutionState> searchTypesBuilder();

        public abstract ImmutableSet.Builder<String> keepSearchTypesBuilder();

        public abstract ExecutionStateGlobalOverride build();
    }
}
