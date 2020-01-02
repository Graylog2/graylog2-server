/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
