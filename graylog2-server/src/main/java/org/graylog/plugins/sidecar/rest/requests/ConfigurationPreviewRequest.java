package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class ConfigurationPreviewRequest {
    @JsonProperty
    public abstract String template();

    @JsonCreator
    public static ConfigurationPreviewRequest create(@JsonProperty("template") String template) {
        return new AutoValue_ConfigurationPreviewRequest(template);
    }
}
