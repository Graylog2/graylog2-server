package org.graylog2.rest.resources.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Set;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputsList {
    @JsonProperty
    public abstract Set<InputStateSummary> inputs();
    @JsonProperty
    public abstract int total();

    public static InputsList create(Set<InputStateSummary> inputs) {
        return new AutoValue_InputsList(inputs, inputs.size());
    }
}
