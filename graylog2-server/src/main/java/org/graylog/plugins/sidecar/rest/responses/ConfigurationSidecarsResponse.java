package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class ConfigurationSidecarsResponse {
    @JsonProperty("configuration_id")
    public abstract String configurationId();

    @JsonProperty("sidecar_ids")
    public abstract Collection<String> sidecarIds();

    @JsonCreator
    public static ConfigurationSidecarsResponse create(@JsonProperty("configuration_id") String configurationId,
                                                       @JsonProperty("sidecar_ids") Collection<String> sidecarIds) {
        return new AutoValue_ConfigurationSidecarsResponse(configurationId, sidecarIds);
    }

}
