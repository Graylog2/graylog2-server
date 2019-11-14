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
import org.joda.time.DateTimeZone;
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

    @Override
    @JsonIgnore
    public DateTime getFrom() { return from(); }
    @JsonIgnore
    public abstract DateTime from();

    @Override
    @JsonIgnore
    public DateTime getTo() { return to(); }
    @JsonIgnore
    public abstract DateTime to();

    @JsonCreator
    public static RelativeRange create(@JsonProperty("type") String type, @JsonProperty("range") int range) throws InvalidRangeParametersException {
        return builder().type(type).range(range).build();
    }

    public static RelativeRange create(int range) throws InvalidRangeParametersException {
        return create(RELATIVE, range);
    }

    @JsonCreator
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
        public abstract RelativeRange autoBuild();

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder range(int range);
        abstract int range();

        abstract Builder from(DateTime from);

        abstract Builder to(DateTime from);

        public RelativeRange build() throws InvalidRangeParametersException {
            if (range() < 0) {
                throw new InvalidRangeParametersException("Range must not be negative");
            }

            return from(range() > 0 ? Tools.nowUTC().minus(Seconds.seconds(range())) : new DateTime(0, DateTimeZone.UTC))
                    .to(Tools.nowUTC())
                    .autoBuild();
        }
    }

}
