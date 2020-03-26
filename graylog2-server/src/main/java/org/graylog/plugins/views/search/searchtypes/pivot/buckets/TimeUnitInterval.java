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
import com.google.auto.value.AutoValue;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

@AutoValue
@JsonTypeName(TimeUnitInterval.type)
@JsonDeserialize(builder = TimeUnitInterval.Builder.class)
public abstract class TimeUnitInterval implements Interval {
    public static final String type = "timeunit";
    public static final Pattern TIMEUNIT_PATTERN = Pattern.compile("(?<quantity>\\d+)(?<unit>[smhdwM])");

    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String timeunit();

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        return new DateHistogramInterval(adjustUnitsLongerThanDays(timeunit()));
    }

    private String adjustUnitsLongerThanDays(String timeunit) {
        final Matcher matcher = TIMEUNIT_PATTERN.matcher(timeunit());
        checkArgument(matcher.matches(),
                "Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].");
        final int quantity = Integer.parseInt(matcher.group("quantity"));
        final String unit = matcher.group("unit");

        switch (unit) {
            case "s":
            case "m":
            case "h":
            case "d": return timeunit;
            case "w": return quantity == 1 ? timeunit : (7 * quantity) + "d";
            case "M": return quantity == 1 ? timeunit : (30 * quantity) + "d";
            default: throw new RuntimeException("Invalid time unit: " + timeunit);
        }
    }

    public static TimeUnitInterval ofTimeunit(String timeunit) {
        return Builder.builder().timeunit(timeunit).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("timeunit")
        public abstract Builder timeunit(String timeunit);

        abstract TimeUnitInterval autoBuild();
        public TimeUnitInterval build() {
            final TimeUnitInterval interval = autoBuild();
            final Matcher matcher = TIMEUNIT_PATTERN.matcher(interval.timeunit());
            checkArgument(matcher.matches(),
                    "Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].");

            final int quantity = Integer.parseInt(matcher.group("quantity"), 10);
            checkArgument(quantity > 0,
                    "Time unit's value must be a positive number, greater than zero.");

            return interval;
        }

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_TimeUnitInterval.Builder().type(type);
        }

        @JsonCreator
        public static Builder createForLegacySingleString(String timeunit) {
            return builder().timeunit(timeunit);
        }
    }
}
