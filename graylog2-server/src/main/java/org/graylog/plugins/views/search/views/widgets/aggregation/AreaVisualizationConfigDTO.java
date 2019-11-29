package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(AreaVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = AreaVisualizationConfigDTO.Builder.class)
public abstract class AreaVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "area";
    private static final String FIELD_INTERPOLATION = "interpolation";

    @JsonProperty(FIELD_INTERPOLATION)
    public abstract Interpolation interpolation();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_INTERPOLATION)
        public abstract Builder interpolation(Interpolation interpolation);

        public abstract AreaVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_AreaVisualizationConfigDTO.Builder()
                    .interpolation(Interpolation.defaultValue());
        }
    }
}
