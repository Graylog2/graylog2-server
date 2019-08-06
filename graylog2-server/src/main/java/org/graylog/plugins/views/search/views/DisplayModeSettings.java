package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = DisplayModeSettings.Builder.class)
public abstract class DisplayModeSettings {
    private static final String FIELD_POSITIONS = "positions";

    @JsonProperty(FIELD_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> positions();

    public static DisplayModeSettings empty() {
        return Builder.create().build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_POSITIONS)
        public abstract Builder positions(Map<String, WidgetPositionDTO> positions);

        public abstract DisplayModeSettings build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_DisplayModeSettings.Builder()
                    .positions(Collections.emptyMap());
        }
    }
}
