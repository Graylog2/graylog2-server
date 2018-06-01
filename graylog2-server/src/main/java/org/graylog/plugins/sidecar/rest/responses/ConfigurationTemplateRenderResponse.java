package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ConfigurationTemplateRenderResponse {
    @JsonProperty
    public abstract String configuration();

    @JsonCreator
    public static ConfigurationTemplateRenderResponse create(
            @JsonProperty("configuration") String configuration) {
        return new AutoValue_ConfigurationTemplateRenderResponse(configuration);
    }

}
