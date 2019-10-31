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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = AggregationSeriesValue.Builder.class)
public abstract class AggregationSeriesValue {
    public abstract AggregationSeries series();

    public abstract ImmutableList<String> key();

    public abstract double value();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public AggregationSeriesValue withoutFirstBucket() {
        return toBuilder().key(key().stream().skip(1).collect(Collectors.toList())).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationSeriesValue.Builder();
        }

        public abstract Builder series(AggregationSeries series);

        public abstract Builder key(List<String> key);

        public abstract Builder value(double value);

        public abstract AggregationSeriesValue build();
    }
}
