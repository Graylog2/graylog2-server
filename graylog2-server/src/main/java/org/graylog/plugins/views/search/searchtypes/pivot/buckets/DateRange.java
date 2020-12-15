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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = DateRange.Builder.class)
public abstract class DateRange {


    @JsonProperty
    public abstract Optional<DateTime> from();

    @JsonProperty
    public abstract Optional<DateTime> to();

    public static DateRange create(@Nullable DateTime from, @Nullable DateTime to) {
        final Builder builder = builder();
        if (from != null) {
            builder.from(from);
        }
        if (to != null) {
            builder.to(to);
        }
        return builder.build();
    }

    public static DateRange.Builder builder() {
        return new AutoValue_DateRange.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return DateRange.builder();
        }

        @JsonProperty
        public abstract Builder from(DateTime from);

        @JsonProperty
        public abstract Builder to(DateTime to);

        public abstract DateRange build();
    }

}

