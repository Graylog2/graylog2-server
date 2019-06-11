package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = SeriesDTO.Builder.class)
@WithBeanGetter
public abstract class SeriesDTO {
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_FUNCTION = "function";

    @JsonProperty(FIELD_CONFIG)
    public abstract SeriesConfigDTO config();

    @JsonProperty(FIELD_FUNCTION)
    public abstract String function();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(SeriesConfigDTO config);

        @JsonProperty(FIELD_FUNCTION)
        public abstract Builder function(String function);

        public abstract SeriesDTO build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SeriesDTO.Builder()
                    .config(SeriesConfigDTO.empty());
        }

        @JsonCreator
        public static Builder createFromString(String function) {
            return create().function(function).config(SeriesConfigDTO.empty());
        }
    }
}
