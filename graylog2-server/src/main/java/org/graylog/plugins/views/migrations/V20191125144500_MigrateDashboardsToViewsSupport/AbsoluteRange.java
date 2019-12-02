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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonTypeName(value = AbsoluteRange.ABSOLUTE)
public abstract class AbsoluteRange extends TimeRange {

    static final String ABSOLUTE = "absolute";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract DateTime from();

    @JsonProperty
    public abstract DateTime to();

    static Builder builder() {
        return new AutoValue_AbsoluteRange.Builder();
    }

    @JsonCreator
    static AbsoluteRange create(@JsonProperty("type") String type,
                                       @JsonProperty("from") DateTime from,
                                       @JsonProperty("to") DateTime to) {
        return builder().type(type).from(from).to(to).build();
    }

    static AbsoluteRange create(DateTime from, DateTime to) {
        return builder().type(ABSOLUTE).from(from).to(to).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract AbsoluteRange build();

        abstract Builder type(String type);

        abstract Builder to(DateTime to);

        abstract Builder from(DateTime to);
    }
}
