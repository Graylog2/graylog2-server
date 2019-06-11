package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = Viewport.Builder.class)
public abstract class Viewport {
    @JsonProperty("center_x")
    public abstract double centerX();

    @JsonProperty("center_y")
    public abstract double centerY();

    @JsonProperty
    public abstract int zoom();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("center_x")
        public abstract Builder centerX(double centerX);

        @JsonProperty("center_y")
        public abstract Builder centerY(double centerY);

        @JsonProperty
        public abstract Builder zoom(int zoom);

        public abstract Viewport build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_Viewport.Builder();
        }
    }
}
