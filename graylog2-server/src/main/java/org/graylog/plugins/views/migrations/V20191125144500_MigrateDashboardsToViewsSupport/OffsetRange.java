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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class OffsetRange extends TimeRange {
    static final String OFFSET = "offset";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract String offset();

    static OffsetRange ofSearchTypeId(String searchTypeId) {
        return new AutoValue_OffsetRange(OFFSET, "search_type", Optional.of(searchTypeId), "1i");
    }
}
