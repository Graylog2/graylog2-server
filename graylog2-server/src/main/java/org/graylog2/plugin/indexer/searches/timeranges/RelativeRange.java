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
package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Map;

@AutoValue
@JsonTypeName(RelativeRange.RELATIVE)
public abstract class RelativeRange extends TimeRange {

    public static final String RELATIVE = "relative";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract int range();

    public int getRange() {
        return range();
    }

    @JsonIgnore
    abstract DateTime from();

    @Override
    @JsonIgnore
    public DateTime getFrom() { return from(); };

    @JsonIgnore
    abstract DateTime to();

    @Override
    @JsonIgnore
    public DateTime getTo() { return to(); };

    @JsonCreator
    public static RelativeRange create(@JsonProperty("type") String type, @JsonProperty("range") int range) throws InvalidRangeParametersException {
        return builder().type(type).range(range).build();
    }

    public static RelativeRange create(int range) throws InvalidRangeParametersException {
        return create(RELATIVE, range);
    }

    public static Builder builder() {
        return new AutoValue_RelativeRange.Builder();
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>of(
                "type", RELATIVE,
                "range", getRange());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract RelativeRange autoBuild();

        public RelativeRange build() throws InvalidRangeParametersException {
            final int range = this.range();
            if (range < 0) {
                throw new InvalidRangeParametersException("Range must not be negative");
            }
            final DateTime now = Tools.nowUTC();
            return this.to(now).from(now.minus(Seconds.seconds(range))).autoBuild();
        }

        public abstract Builder type(String type);

        abstract int range();
        public abstract Builder range(int range);

        abstract Builder from(DateTime from);
        abstract Builder to(DateTime to);
    }

}
