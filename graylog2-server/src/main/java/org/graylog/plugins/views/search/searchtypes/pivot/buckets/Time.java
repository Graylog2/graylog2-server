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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonTypeName(Time.NAME)
@JsonDeserialize(builder = Time.Builder.class)
public abstract class Time implements BucketSpec {
    public static final String NAME = "time";
    private static final String FIELD_NULL_VALUES_FOR_EMPTY_BUCKETS = "null_values_for_empty_buckets";

    @Override
    public abstract String type();

    @Override
    @JsonProperty
    public abstract List<String> fields();

    @JsonProperty
    public abstract Interval interval();

    @JsonProperty(FIELD_NULL_VALUES_FOR_EMPTY_BUCKETS)
    public abstract boolean nullValuesForEmptyBuckets();

    public static Time.Builder builder() {
        return new AutoValue_Time.Builder()
                .type(NAME)
                .nullValuesForEmptyBuckets(false);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Time, Builder> {

        @JsonCreator
        public static Builder create() {
            return Time.builder();
        }

        @JsonProperty
        public Builder field(String field) {
            return fields(Collections.singletonList(field));
        }

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public abstract Builder interval(Interval interval);

        public abstract Builder nullValuesForEmptyBuckets(Boolean nullValuesForEmptyBuckets);

        @JsonProperty(FIELD_NULL_VALUES_FOR_EMPTY_BUCKETS)
        public Builder setNullValuesForEmptyBuckets(@Nullable Boolean nullValuesForEmptyBuckets) {
            return nullValuesForEmptyBuckets(firstNonNull(nullValuesForEmptyBuckets, false));
        }
    }
}

