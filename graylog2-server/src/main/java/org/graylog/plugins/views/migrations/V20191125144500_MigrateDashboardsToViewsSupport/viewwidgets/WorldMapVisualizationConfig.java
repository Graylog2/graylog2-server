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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.validation.Valid;

@AutoValue
public abstract class WorldMapVisualizationConfig implements VisualizationConfig {
    public static final String NAME = "map";

    @JsonProperty
    public abstract Viewport viewport();

    private static Builder builder() {
        return new AutoValue_WorldMapVisualizationConfig.Builder();
    }

    public static WorldMapVisualizationConfig create() {
        return WorldMapVisualizationConfig.builder()
                .viewport(Viewport.empty())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("viewport")
        public abstract Builder viewport(@Valid Viewport viewport);

        public abstract WorldMapVisualizationConfig build();
    }
}
