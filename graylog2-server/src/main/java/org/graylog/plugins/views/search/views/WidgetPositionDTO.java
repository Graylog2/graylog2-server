package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = WidgetPositionDTO.Builder.class)
@WithBeanGetter
public abstract class WidgetPositionDTO {
    @JsonProperty("col")
    public abstract Position col();

    @JsonProperty("row")
    public abstract Position row();

    @JsonProperty("height")
    public abstract Position height();

    @JsonProperty("width")
    public abstract Position width();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("col")
        public abstract Builder col(Position col);

        @JsonProperty("row")
        public abstract Builder row(Position row);

        @JsonProperty("height")
        public abstract Builder height(Position height);

        @JsonProperty("width")
        public abstract Builder width(Position width);

        public abstract WidgetPositionDTO build();

        @JsonCreator
        public static Builder create() { return new AutoValue_WidgetPositionDTO.Builder(); }
    }
}
