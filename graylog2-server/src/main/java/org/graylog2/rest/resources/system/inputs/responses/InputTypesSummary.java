package org.graylog2.rest.resources.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputTypesSummary {
    @JsonProperty
    public abstract Map<String, String> types();

    public static InputTypesSummary create(Map<String, String> types) {
        return new AutoValue_InputTypesSummary(types);
    }
}
