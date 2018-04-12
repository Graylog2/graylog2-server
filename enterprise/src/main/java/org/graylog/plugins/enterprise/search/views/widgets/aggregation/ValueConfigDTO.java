package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(ValueConfigDTO.NAME)
@JsonDeserialize(builder = ValueConfigDTO.Builder.class)
public abstract class ValueConfigDTO implements PivotConfigDTO {
    public static final String NAME = "values";

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ValueConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ValueConfigDTO.Builder();
        }
    }
}
