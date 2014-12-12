package org.graylog2.rest.resources.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.shared.inputs.InputDescription;

import java.util.Map;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputTypeInfo {
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String name();
    @JsonProperty
    public abstract boolean isExclusive();
    @JsonProperty
    public abstract Map<String, Map<String, Object>> requestedConfiguration();
    @JsonProperty
    public abstract String linkToDocs();

    public static InputTypeInfo create(String type, InputDescription description) {
        return new AutoValue_InputTypeInfo(type, description.getName(), description.isExclusive(), description.getRequestedConfiguration(), description.getLinkToDocs());
    }
}
