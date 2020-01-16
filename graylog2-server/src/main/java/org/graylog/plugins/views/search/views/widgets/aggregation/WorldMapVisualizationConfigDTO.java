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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.validation.Valid;

@AutoValue
@JsonTypeName(WorldMapVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = WorldMapVisualizationConfigDTO.Builder.class)
public abstract class WorldMapVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "map";

    @JsonProperty
    public abstract Viewport viewport();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("viewport")
        public abstract Builder viewport(@Valid Viewport viewport);

        public abstract WorldMapVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_WorldMapVisualizationConfigDTO.Builder();
        }
    }
}
