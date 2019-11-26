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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(RelativeRange.RELATIVE)
abstract class RelativeRange extends TimeRange {

    static final String RELATIVE = "relative";

    @JsonProperty
    @Override
    abstract String type();

    @JsonProperty
    abstract int range();

    static RelativeRange create(@JsonProperty("type") String type, @JsonProperty("range") int range) {
        return builder().type(type).range(range).build();
    }

    static RelativeRange create(int range) {
        return create(RELATIVE, range);
    }

    static Builder builder() {
        return new AutoValue_RelativeRange.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract RelativeRange build();

        abstract Builder type(String type);

        abstract Builder range(int range);
    }

}
