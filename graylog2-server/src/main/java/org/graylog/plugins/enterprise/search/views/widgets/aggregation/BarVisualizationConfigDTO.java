package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(BarVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = BarVisualizationConfigDTO.Builder.class)
public abstract class BarVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "bar";
    private static final String FIELD_BAR_MODE = "barmode";

    public enum BarMode {
        stack,
        overlay,
        group,
        relative
    };

    @JsonProperty
    public abstract BarMode barmode();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_BAR_MODE)
        public abstract Builder barmode(BarMode barMode);

        public abstract BarVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_BarVisualizationConfigDTO.Builder();
        }
    }
}
