package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = SeriesConfigDTO.Builder.class)
@WithBeanGetter
public abstract class SeriesConfigDTO {
    static final String FIELD_NAME = "name";

    public static SeriesConfigDTO empty() {
        return Builder.builder().build();
    }

    @JsonProperty(FIELD_NAME)
    @Nullable
    public abstract String name();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        @Nullable
        public abstract Builder name(String name);

        public abstract SeriesConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_SeriesConfigDTO.Builder();
        }
    }
}
