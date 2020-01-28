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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = ChartColorSetting.Builder.class)
public abstract class ChartColorSetting {
   private static final String FIELD_NAME = "field_name";
    private static final String FIELD_CHART_COLOR = "chart_color";

    @JsonProperty(FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_CHART_COLOR)
    public abstract ChartColor chartColor();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        public abstract Builder fieldName(String widgetId);

        @JsonProperty(FIELD_CHART_COLOR)
        public abstract Builder chartColor(ChartColor chartColor);

        public abstract ChartColorSetting build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ChartColorSetting.Builder();
        }
    }
}
