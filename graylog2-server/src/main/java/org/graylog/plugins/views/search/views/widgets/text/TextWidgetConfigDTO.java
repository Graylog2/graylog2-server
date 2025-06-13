package org.graylog.plugins.views.search.views.widgets.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;

@AutoValue
@JsonTypeName(TextWidgetConfigDTO.NAME)
@JsonDeserialize(builder = TextWidgetConfigDTO.Builder.class)
public abstract class TextWidgetConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "text";
    private static final String FIELD_TEXT = "text";

    @JsonProperty(FIELD_TEXT)
    public abstract String text();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_TEXT)
        public abstract Builder text(String text);

        public abstract TextWidgetConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_TextWidgetConfigDTO.Builder();
        }
    }
}
