package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ConfigurationPreviewRenderResponse {
    @JsonProperty
    public abstract String preview();

    @JsonCreator
    public static ConfigurationPreviewRenderResponse create(
            @JsonProperty("preview") String preview) {
        return new AutoValue_ConfigurationPreviewRenderResponse(preview);
    }

}
