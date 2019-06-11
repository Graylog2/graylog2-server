package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.validation.Valid;

@AutoValue
@JsonTypeName(WorldMapVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = WorldMapVisualizationConfigDTO.Builder.class)
public abstract class WorldMapVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "map";

    @JsonProperty
    public abstract Viewport viewport();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("viewport")
        public abstract Builder viewport(@Valid Viewport viewport);

        public abstract WorldMapVisualizationConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_WorldMapVisualizationConfigDTO.Builder();
        }
    }
}
