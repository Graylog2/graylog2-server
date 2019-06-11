package org.graylog.plugins.enterprise.search.views;

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