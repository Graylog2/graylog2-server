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
import org.joda.time.Period;

@JsonAutoDetect
@AutoValue
public abstract class SearchesClusterConfig {
    private static final Period DEFAULT_QUERY_TIME_RANGE_LIMIT = Period.ZERO;

    @JsonProperty("query_time_range_limit")
    public abstract Period queryTimeRangeLimit();

    @JsonCreator
    public static SearchesClusterConfig create(@JsonProperty("query_time_range_limit") Period queryTimeRangeLimit) {
        return builder()
                .queryTimeRangeLimit(queryTimeRangeLimit)
                .build();
    }

    public static SearchesClusterConfig createDefault() {
        return builder()
                .queryTimeRangeLimit(DEFAULT_QUERY_TIME_RANGE_LIMIT)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SearchesClusterConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder queryTimeRangeLimit(Period queryTimeRangeLimit);

        public abstract SearchesClusterConfig build();
    }
}
