package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonTypeName(AutoIntervalDTO.type)
public abstract class AutoIntervalDTO implements IntervalDTO {
    public static final String type = "auto";

    @JsonProperty
    public abstract String type();

    @JsonCreator
    public static AutoIntervalDTO create() {
        return new AutoValue_AutoIntervalDTO(type);
    }
}
