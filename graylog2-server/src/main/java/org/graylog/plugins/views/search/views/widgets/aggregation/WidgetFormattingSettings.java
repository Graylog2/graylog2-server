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

import java.util.Collections;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = WidgetFormattingSettings.Builder.class)
public abstract class WidgetFormattingSettings {
    private static final String FIELD_CHART_COLORS = "chart_colors";

    @JsonProperty(FIELD_CHART_COLORS)
    public abstract List<ChartColorSetting> chartColorSettings();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_CHART_COLORS)
        public abstract Builder chartColorSettings(List<ChartColorSetting> chartColorSettings);

        public abstract WidgetFormattingSettings build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_WidgetFormattingSettings.Builder()
                    .chartColorSettings(Collections.emptyList());
        }
    }
}
