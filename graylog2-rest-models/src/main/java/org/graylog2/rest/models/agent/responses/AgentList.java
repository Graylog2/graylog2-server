package org.graylog2.rest.models.agent.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class AgentList {
    @JsonProperty
    public abstract List<AgentSummary> agents();

    @JsonCreator
    public static AgentList create(@JsonProperty("agents") List<AgentSummary> agents) {
        return new AutoValue_AgentList(agents);
    }
}
