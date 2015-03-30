package org.graylog2.rest.models.agent.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class AgentSummary {

    @JsonCreator
    public static AgentSummary create() {
        return new AutoValue_AgentSummary();
    }
}
