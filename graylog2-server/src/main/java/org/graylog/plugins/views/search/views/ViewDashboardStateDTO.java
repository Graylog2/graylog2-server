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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = ViewDashboardStateDTO.Builder.class)
public abstract class ViewDashboardStateDTO {
    private static final String FIELD_WIDGETS = "widgets";
    private static final String FIELD_POSITIONS = "positions";

    @JsonProperty(FIELD_WIDGETS)
    public abstract Map<String, DashboardWidgetDTO> widgets();

    @JsonProperty(FIELD_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> positions();

    public static ViewDashboardStateDTO empty() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewDashboardStateDTO.Builder()
                    .widgets(Collections.emptyMap())
                    .positions(Collections.emptyMap());
        }

        @JsonProperty(FIELD_WIDGETS)
        public abstract Builder widgets(Map<String, DashboardWidgetDTO> widgets);

        @JsonProperty(FIELD_POSITIONS)
        public abstract Builder positions(Map<String, WidgetPositionDTO> positions);

        public abstract ViewDashboardStateDTO build();
    }
}
