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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import javax.annotation.Nullable;
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
        public abstract Builder bucketKey(@Nullable BucketKey key);
    }

}

