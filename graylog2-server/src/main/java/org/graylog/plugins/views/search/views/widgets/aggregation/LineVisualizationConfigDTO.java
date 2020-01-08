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

@AutoValue
@JsonTypeName(LineVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = LineVisualizationConfigDTO.Builder.class)
public abstract class LineVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "line";
    private static final String FIELD_INTERPOLATION = "interpolation";

    @JsonProperty(FIELD_INTERPOLATION)
    public abstract Interpolation interpolation();

    public static Builder builder() {
        return new AutoValue_LineVisualizationConfigDTO.Builder()
                .interpolation(Interpolation.defaultValue());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_INTERPOLATION)
        public abstract Builder interpolation(Interpolation interpolation);

        public abstract LineVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_LineVisualizationConfigDTO.Builder()
                    .interpolation(Interpolation.defaultValue());
        }
    }
}
