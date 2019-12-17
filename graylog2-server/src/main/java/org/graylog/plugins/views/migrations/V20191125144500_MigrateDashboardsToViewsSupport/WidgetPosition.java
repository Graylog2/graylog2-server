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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = WidgetPosition.Builder.class)
public abstract class WidgetPosition {

    @JsonProperty("width")
    public abstract int width();

    @JsonProperty("height")
    public abstract int height();

    @JsonProperty("col")
    public abstract int col();

    @JsonProperty("row")
    public abstract int row();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract WidgetPosition build();

        @JsonProperty("width")
        public abstract Builder width(int width);

        @JsonProperty("height")
        public abstract Builder height(int height);

        @JsonProperty("col")
        public abstract Builder col(int col);

        @JsonProperty("row")
        public abstract Builder row(int row);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_WidgetPosition.Builder();
        }
    }
}
