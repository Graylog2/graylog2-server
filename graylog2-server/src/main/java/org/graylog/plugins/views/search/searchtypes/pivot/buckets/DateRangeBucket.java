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

import java.util.List;

@AutoValue
@JsonTypeName(DateRangeBucket.NAME)
@JsonDeserialize(builder = DateRangeBucket.Builder.class)
public abstract class DateRangeBucket implements BucketSpec {
    public static final String NAME = "date_range";

    public enum BucketKey {
        @JsonProperty("from")
        FROM,
        @JsonProperty("to")
        TO
    }

    @JsonProperty
    public abstract BucketKey bucketKey();

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract List<DateRange> ranges();

    public static DateRangeBucket.Builder builder() {
        return new AutoValue_DateRangeBucket.Builder()
                .type(NAME)
                .bucketKey(BucketKey.TO);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<DateRangeBucket, Builder> {

        @JsonCreator
        public static Builder create() {
            return DateRangeBucket.builder();
        }

        @JsonProperty
        public abstract Builder field(String field);

        @JsonProperty
        public abstract Builder ranges(List<DateRange> ranges);

        @JsonProperty
        public abstract Builder bucketKey(BucketKey key);
    }

}

