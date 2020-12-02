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
package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class AggregationSeries {
    private static final String FIELD_ID = "id";
    private static final String FIELD_FUNCTION = "function";
    private static final String FIELD_FIELD = "field";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_FUNCTION)
    public abstract AggregationFunction function();

    @JsonProperty(FIELD_FIELD)
    public abstract Optional<String> field();

    public static Builder builder() {
        return new AutoValue_AggregationSeries.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder function(AggregationFunction function);

        public abstract Builder field(@Nullable String field);

        abstract Optional<String> field();
        abstract AggregationSeries autoBuild();

        public AggregationSeries build() {
            // Most of the views code doesn't handle empty strings. Best to convert them here.
            // See: https://github.com/Graylog2/graylog2-server/issues/6933#issuecomment-568447111
            // TODO: It would be cleaner to use validations like "@NotBlank" and fix the frontend to send
            //       "null" instead of an empty string. This requires an auto-value update and some other
            //       modifications but we didn't want to do this in 3.2-beta.
            if (field().isPresent() && field().get().isEmpty()) {
                field(null);
            }
            return autoBuild();
        }
    }

    @JsonCreator
    public static AggregationSeries create(@JsonProperty(FIELD_ID) String id,
                                           @JsonProperty(FIELD_FUNCTION) AggregationFunction function,
                                           @JsonProperty(FIELD_FIELD) @Nullable String field) {
        return builder()
                .id(Optional.ofNullable(id).orElse(new ObjectId().toHexString()))
                .function(function)
                .field(field)
                .build();
    }
}
