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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Viewport {
    @JsonProperty("center_x")
    public abstract double centerX();

    @JsonProperty("center_y")
    public abstract double centerY();

    @JsonProperty
    public abstract int zoom();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("center_x")
        public abstract Builder centerX(double centerX);

        @JsonProperty("center_y")
        public abstract Builder centerY(double centerY);

        @JsonProperty
        public abstract Builder zoom(int zoom);

        public abstract Viewport build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_Viewport.Builder();
        }
    }
}
