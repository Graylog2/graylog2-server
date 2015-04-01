package org.graylog2.agents;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.agent.AgentNodeDetailsSummary;

@AutoValue
@JsonAutoDetect
public abstract class AgentNodeDetails {
    @JsonProperty("operating_system")
    public abstract String operatingSystem();

    @JsonCreator
    public static AgentNodeDetails create(@JsonProperty("operating_system") String operatingSystem) {
        return new AutoValue_AgentNodeDetails(operatingSystem);
    }

    public AgentNodeDetailsSummary toSummary() {
        return AgentNodeDetailsSummary.create(operatingSystem());
    }
}
