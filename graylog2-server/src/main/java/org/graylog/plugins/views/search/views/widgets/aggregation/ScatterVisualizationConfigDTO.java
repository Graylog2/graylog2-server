package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(ScatterVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = ScatterVisualizationConfigDTO.Builder.class)
public abstract class ScatterVisualizationConfigDTO implements VisualizationConfigDTO, XYVisualizationConfig{
    public static final String NAME = "scatter";

    @JsonProperty(FIELD_AXIS_TYPE)
    public abstract XYVisualizationConfig.AxisType axisType();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_AXIS_TYPE)
        public abstract Builder axisType(AxisType axisType);

        public abstract ScatterVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_ScatterVisualizationConfigDTO.Builder()
                    .axisType(DEFAULT_AXIS_TYPE);
        }

    }
}
