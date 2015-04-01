package org.graylog2.agents;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
@CollectionName("agents")
public abstract class AgentImpl implements Agent {

    @JsonProperty("id")
    @Override
    public abstract String getId();

    @JsonProperty("node_id")
    @Override
    public abstract String getNodeId();

    @JsonProperty("node_details")
    public abstract AgentNodeDetails nodeDetails();

    @Override
    public AgentSummary toSummary() {
        return AgentSummary.create(getId(), getNodeId(), nodeDetails().toSummary(), getLastSeen());
    }

    @Override
    @JsonIgnore
    public String getOperatingSystem() {
        return nodeDetails().operatingSystem();
    }

    @JsonProperty("last_seen")
    @Override
    public abstract DateTime getLastSeen();

    @JsonCreator
    public static AgentImpl create(@JsonProperty("_id") String objectId,
                                   @JsonProperty("id") String id,
                                   @JsonProperty("node_id") String nodeId,
                                   @JsonProperty("node_details") AgentNodeDetails agentNodeDetails,
                                   @JsonProperty("last_seen") DateTime lastSeen) {
        return new AutoValue_AgentImpl(id, nodeId, agentNodeDetails, lastSeen);
    }

    public static AgentImpl create(String agentId, String nodeId, AgentNodeDetails agentNodeDetails, DateTime lastSeen) {
        return new AutoValue_AgentImpl(agentId, nodeId, agentNodeDetails, lastSeen);
    }
}
