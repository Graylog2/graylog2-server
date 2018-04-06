package org.graylog.plugins.enterprise.search.views.widgets.alertstatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.views.WidgetConfigDTO;

@AutoValue
@JsonTypeName(AlertStatusConfigDTO.NAME)
@JsonDeserialize(builder = AlertStatusConfigDTO.Builder.class)
public abstract class AlertStatusConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "alert_status";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_TRIGGERED = "triggered";
    private static final String FIELD_BG_COLOR = "bg_color";
    private static final String FIELD_TRIGGERED_BG_COLOR = "triggered_bg_color";
    private static final String FIELD_TEXT = "text";

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_TRIGGERED)
    public abstract boolean triggered();

    @JsonProperty(FIELD_BG_COLOR)
    public abstract String bgColor();

    @JsonProperty(FIELD_TRIGGERED_BG_COLOR)
    public abstract String triggeredBgColor();

    @JsonProperty(FIELD_TEXT)
    public abstract String text();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_TRIGGERED)
        public abstract Builder triggered(boolean triggered);

        @JsonProperty(FIELD_BG_COLOR)
        public abstract Builder bgColor(String bgColor);

        @JsonProperty(FIELD_TRIGGERED_BG_COLOR)
        public abstract Builder triggeredBgColor(String triggeredBgColor);

        @JsonProperty(FIELD_TEXT)
        public abstract Builder text(String text);

        public abstract AlertStatusConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_AlertStatusConfigDTO.Builder();
        }
    }
}
