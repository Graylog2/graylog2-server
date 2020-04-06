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

