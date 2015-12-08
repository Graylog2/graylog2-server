package org.graylog2.rest.models.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class InputStatesList {
    @JsonProperty("states")
    public abstract Set<InputStateSummary> states();

    @JsonCreator
    public static InputStatesList create(@JsonProperty("states") Set<InputStateSummary> states) {
        return new AutoValue_InputStatesList(states);
    }
}
