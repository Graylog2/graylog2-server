package org.graylog.plugins.enterprise.search.views;

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
    public abstract int col();

    @JsonProperty("row")
    public abstract int row();

    @JsonProperty("height")
    public abstract int height();

    @JsonProperty("width")
    public abstract int width();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("col")
        public abstract Builder col(int col);

        @JsonProperty("row")
        public abstract Builder row(int row);

        @JsonProperty("height")
        public abstract Builder height(int height);

        @JsonProperty("width")
        public abstract Builder width(int width);

        public abstract WidgetPositionDTO build();

        @JsonCreator
        public static Builder create() { return new AutoValue_WidgetPositionDTO.Builder(); }
    }
}
