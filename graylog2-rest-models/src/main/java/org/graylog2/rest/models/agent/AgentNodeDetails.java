package org.graylog2.rest.models.agent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class AgentNodeDetails {

    @JsonProperty("operating_system")
    public abstract String operatingSystem();

    @JsonCreator
    public static AgentNodeDetails create(@JsonProperty("operating_system") String operatingSystem) {
        return new AutoValue_AgentNodeDetails(operatingSystem);
    }
}
