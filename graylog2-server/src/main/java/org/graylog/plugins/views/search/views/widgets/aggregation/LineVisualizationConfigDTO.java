package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(LineVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = LineVisualizationConfigDTO.Builder.class)
public abstract class LineVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "line";
    private static final String FIELD_INTERPOLATION = "interpolation";

    @JsonProperty(FIELD_INTERPOLATION)
    public abstract Interpolation interpolation();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_INTERPOLATION)
        public abstract Builder interpolation(Interpolation interpolation);

        public abstract LineVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_LineVisualizationConfigDTO.Builder()
                    .interpolation(Interpolation.defaultValue());
        }
    }
}
