package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(TimeHistogramConfigDTO.NAME)
@JsonDeserialize(builder = TimeHistogramConfigDTO.Builder.class)
public abstract class TimeHistogramConfigDTO implements PivotConfigDTO {
    public static final String NAME = "time";
    static final String FIELD_INTERVAL = "interval";

    @JsonProperty(FIELD_INTERVAL)
    public abstract IntervalDTO interval();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_INTERVAL)
        public abstract Builder interval(IntervalDTO interval);

        public abstract TimeHistogramConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_TimeHistogramConfigDTO.Builder();
        }
    }
}
