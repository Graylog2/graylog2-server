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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = SearchTypeExecutionState.Builder.class)
public abstract class SearchTypeExecutionState {

    public static SearchTypeExecutionState from(ExecutionStateGlobalOverride globalOverride) {
        final Builder builder = builder();
        globalOverride.limit().ifPresent(builder::limit);
        globalOverride.offset().ifPresent(builder::offset);
        return builder.build();
    }

    @JsonProperty
    public abstract Optional<Integer> limit();
    @JsonProperty
    public abstract Optional<Integer> offset();

    @Nullable
    @JsonProperty("after")
    public abstract List<Object> searchAfter();

    public static SearchTypeExecutionState.Builder builder() {
        return new AutoValue_SearchTypeExecutionState.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract Builder limit(Integer limit);
        @JsonProperty
        public abstract Builder offset(Integer offset);
        @JsonProperty("after")
        public abstract Builder searchAfter(@Nullable List<Object> searchAfter);

        @JsonCreator
        public static Builder create() {
            return SearchTypeExecutionState.builder();
        }

        public abstract SearchTypeExecutionState build();
    }
}
