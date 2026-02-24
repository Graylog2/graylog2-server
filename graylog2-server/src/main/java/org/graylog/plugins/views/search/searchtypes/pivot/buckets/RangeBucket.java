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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import java.util.Collections;
import java.util.List;

@AutoValue
@JsonTypeName(RangeBucket.NAME)
@JsonDeserialize(builder = RangeBucket.Builder.class)
public abstract class RangeBucket implements BucketSpec {
    public static final String NAME = "range";

    @Override
    public abstract String type();

    @Override
    @JsonProperty
    public abstract List<String> fields();

    @JsonProperty
    public abstract List<NumberRange> ranges();

    public static RangeBucket.Builder builder() {
        return new AutoValue_RangeBucket.Builder()
                .type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<RangeBucket, Builder> {
        @JsonCreator
        public static Builder create() {
            return RangeBucket.builder();
        }

        @JsonProperty
        public Builder field(String field) {
            return fields(Collections.singletonList(field));
        }

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public abstract Builder ranges(List<NumberRange> ranges);
    }
}
