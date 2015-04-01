package org.graylog2.rest.models.agent.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.agent.AgentNodeDetailsSummary;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class AgentSummary {

    @JsonProperty
    public abstract String id();

    @JsonProperty("node_id")
    public abstract String nodeId();

    @JsonProperty("node_details")
    public abstract AgentNodeDetailsSummary nodeDetails();

    @JsonProperty("last_seen")
    public abstract DateTime lastSeen();

    @JsonCreator
    public static AgentSummary create(@JsonProperty("id") String id,
                                      @JsonProperty("node_id") String nodeId,
                                      @JsonProperty("node_details") AgentNodeDetailsSummary nodeDetails,
                                      @JsonProperty("last_seen") DateTime lastSeen) {
        return new AutoValue_AgentSummary(id, nodeId, nodeDetails, lastSeen);
    }
}
