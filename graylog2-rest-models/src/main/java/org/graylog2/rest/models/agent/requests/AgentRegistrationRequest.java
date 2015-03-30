package org.graylog2.rest.models.agent.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class AgentRegistrationRequest {

    @JsonCreator
    public static AgentRegistrationRequest create() {
        return new AutoValue_AgentRegistrationRequest();
    }
}
