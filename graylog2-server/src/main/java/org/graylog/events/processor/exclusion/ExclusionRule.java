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
package org.graylog.events.processor.exclusion;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ExclusionRule.Builder.class)
public abstract class ExclusionRule {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_MATCHERS = "matchers";

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_MATCHERS)
    public abstract ImmutableList<Matcher> matchers();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ExclusionRule.Builder();
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(@Nullable String title);

        @JsonProperty(FIELD_MATCHERS)
        public abstract Builder matchers(ImmutableList<Matcher> matchers);

        abstract ExclusionRule autoBuild();

        public ExclusionRule build() {
            final ExclusionRule rule = autoBuild();
            if (rule.matchers().isEmpty()) {
                throw new IllegalArgumentException("ExclusionRule must contain at least one matcher");
            }
            return rule;
        }
    }
}
