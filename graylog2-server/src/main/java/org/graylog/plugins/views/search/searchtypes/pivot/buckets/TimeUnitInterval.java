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

@AutoValue
@JsonTypeName(TimeUnitInterval.type)
@JsonDeserialize(builder = TimeUnitInterval.Builder.class)
public abstract class TimeUnitInterval implements Interval {
    public static final String type = "timeunit";

    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String timeunit();

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        return new DateHistogramInterval(timeunit());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("timeunit")
        public abstract Builder timeunit(String timeunit);

        public abstract TimeUnitInterval build();

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
