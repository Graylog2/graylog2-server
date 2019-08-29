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
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = ViewStateDTO.Builder.class)
@WithBeanGetter
public abstract class ViewStateDTO {
    static final String FIELD_SELECTED_FIELDS = "selected_fields";
    static final String FIELD_STATIC_MESSAGE_LIST_ID = "static_message_list_id";
    static final String FIELD_TITLES = "titles";
    static final String FIELD_WIDGETS = "widgets";
    static final String FIELD_WIDGET_MAPPING = "widget_mapping";
    static final String FIELD_WIDGET_POSITIONS = "positions";
    static final String FIELD_FORMATTING = "formatting";
    static final String FIELD_DISPLAY_MODE_SETTINGS = "display_mode_settings";

    @JsonProperty(FIELD_SELECTED_FIELDS)
    public abstract Set<String> fields();

    @Nullable
    @JsonProperty(FIELD_STATIC_MESSAGE_LIST_ID)
    public abstract String staticMessageListId();

    @JsonProperty(FIELD_TITLES)
    public abstract Map<String, Map<String, String>> titles();

    @JsonProperty(FIELD_WIDGETS)
    public abstract Set<WidgetDTO> widgets();

    @JsonProperty(FIELD_WIDGET_MAPPING)
    public abstract Map<String, Set<String>> widgetMapping();

    @JsonProperty(FIELD_WIDGET_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> widgetPositions();

    @JsonProperty(FIELD_FORMATTING)
    @Nullable
    public abstract FormattingSettings formatting();

    @JsonProperty(FIELD_DISPLAY_MODE_SETTINGS)
    public abstract DisplayModeSettings displayModeSettings();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_SELECTED_FIELDS)
        public abstract Builder fields(Set<String> fields);

        @Nullable
        @JsonProperty(FIELD_STATIC_MESSAGE_LIST_ID)
        public abstract Builder staticMessageListId(String staticMessageListId);

        @JsonProperty(FIELD_TITLES)
        public abstract Builder titles(Map<String, Map<String, String>> titles);

        @JsonProperty(FIELD_WIDGETS)
        public abstract Builder widgets(Set<WidgetDTO> widgets);

        @JsonProperty(FIELD_WIDGET_MAPPING)
        public abstract Builder widgetMapping(Map<String, Set<String>> widgetMapping);

        @JsonProperty(FIELD_WIDGET_POSITIONS)
        public abstract Builder widgetPositions(Map<String, WidgetPositionDTO> widgetPositions);

        @JsonProperty(FIELD_FORMATTING)
        public abstract Builder formatting(FormattingSettings formattingSettings);

        @JsonProperty(FIELD_DISPLAY_MODE_SETTINGS)
        public abstract Builder displayModeSettings(DisplayModeSettings displayModeSettings);

        public abstract ViewStateDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewStateDTO.Builder()
                    .titles(Collections.emptyMap())
                    .displayModeSettings(DisplayModeSettings.empty());
        }
    }
}
