package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(ValueConfigDTO.NAME)
@JsonDeserialize(builder = ValueConfigDTO.Builder.class)
public abstract class ValueConfigDTO implements PivotConfigDTO {
    public static final String NAME = "values";
    static final String FIELD_LIMIT = "limit";

    @JsonProperty(FIELD_LIMIT)
    public abstract int limit();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_LIMIT)
        public abstract Builder limit(int limit);

        public abstract ValueConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ValueConfigDTO.Builder().limit(15);
        }
    }
}
