package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = ViewStateDTO.Builder.class)
@WithBeanGetter
public abstract class ViewStateDTO {
    static final String FIELD_SELECTED_FIELDS = "selected_fields";
    static final String FIELD_TITLES = "titles";
    static final String FIELD_WIDGETS = "widgets";
    static final String FIELD_WIDGET_MAPPING = "widget_mapping";
    static final String FIELD_WIDGET_POSITIONS = "positions";

    @JsonProperty(FIELD_SELECTED_FIELDS)
    public abstract Set<String> fields();

    @JsonProperty(FIELD_TITLES)
    public abstract Map<String, Map<String, String>> titles();

    @JsonProperty(FIELD_WIDGETS)
    public abstract Set<WidgetDTO> widgets();

    @JsonProperty(FIELD_WIDGET_MAPPING)
    public abstract Map<String, Set<String>> widgetMapping();

    @JsonProperty(FIELD_WIDGET_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> widgetPositions();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_SELECTED_FIELDS)
        public abstract Builder fields(Set<String> fields);

        @JsonProperty(FIELD_TITLES)
        public abstract Builder titles(Map<String, Map<String, String>> titles);

        @JsonProperty(FIELD_WIDGETS)
        public abstract Builder widgets(Set<WidgetDTO> widgets);

        @JsonProperty(FIELD_WIDGET_MAPPING)
        public abstract Builder widgetMapping(Map<String, Set<String>> widgetMapping);

        @JsonProperty(FIELD_WIDGET_POSITIONS)
        public abstract Builder widgetPositions(Map<String, WidgetPositionDTO> widgetPositions);

        public abstract ViewStateDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewStateDTO.Builder().titles(Collections.emptyMap());
        }
    }
}
