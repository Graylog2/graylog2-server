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
package org.graylog2.indexer.searches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class SearchesClusterConfig {
    private static final org.joda.time.format.PeriodFormatter ISO_PERIOD_FORMAT = ISOPeriodFormat.standard();

    private static final Period DEFAULT_QUERY_TIME_RANGE_LIMIT = Period.ZERO;
    private static final Map<Period, String> DEFAULT_RELATIVE_TIMERANGE_OPTIONS = ImmutableMap.<Period, String>builder()
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT5M"), "Search in the last 5 minutes")
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT15M"), "Search in the last 15 minutes")
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT30M"), "Search in the last 30 minutes")
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT1H"), "Search in the last 1 hour")
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT2H"), "Search in the last 2 hours")
            .put(ISO_PERIOD_FORMAT.parsePeriod("PT8H"), "Search in the last 8 hours")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P1D"), "Search in the last 1 day")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P2D"), "Search in the last 2 days")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P5D"), "Search in the last 5 days")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P7D"), "Search in the last 7 days")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P14D"), "Search in the last 14 days")
            .put(ISO_PERIOD_FORMAT.parsePeriod("P30D"), "Search in the last 30 days")
            .put(Period.ZERO, "Search in all messages")
            .build();

    @JsonProperty("query_time_range_limit")
    public abstract Period queryTimeRangeLimit();

    @JsonProperty("relative_timerange_options")
    public abstract Map<Period, String> relativeTimerangeOptions();

    @JsonCreator
    public static SearchesClusterConfig create(@JsonProperty("query_time_range_limit") Period queryTimeRangeLimit,
                                               @JsonProperty("relative_timerange_options") Map<Period, String> relativeTimerangeOptions) {
        return builder()
                .queryTimeRangeLimit(queryTimeRangeLimit)
                .relativeTimerangeOptions(relativeTimerangeOptions)
                .build();
    }

    public static SearchesClusterConfig createDefault() {
        return builder()
                .queryTimeRangeLimit(DEFAULT_QUERY_TIME_RANGE_LIMIT)
                .relativeTimerangeOptions(DEFAULT_RELATIVE_TIMERANGE_OPTIONS)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SearchesClusterConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder queryTimeRangeLimit(Period queryTimeRangeLimit);
        public abstract Builder relativeTimerangeOptions(Map<Period, String> relativeTimerangeOptions);

        public abstract SearchesClusterConfig build();
    }
}
