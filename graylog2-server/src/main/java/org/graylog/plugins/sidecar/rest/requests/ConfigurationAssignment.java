package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class ConfigurationAssignment {
    @JsonProperty
    public abstract String collectorId();

    @JsonProperty
    public abstract String configurationId();

    @JsonCreator
    public static ConfigurationAssignment create(@JsonProperty("collector_id") String collectorId,
                                                 @JsonProperty("configuration_id") String configurationId) {
        return new AutoValue_ConfigurationAssignment(collectorId, configurationId);
    }
}
