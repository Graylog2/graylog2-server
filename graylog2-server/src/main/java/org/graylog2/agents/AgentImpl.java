/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.agents;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
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
    public abstract AgentNodeDetails getNodeDetails();

    @JsonProperty("agent_version")
    public abstract String getAgentVersion();

    @Override
    public AgentSummary toSummary(Function<Agent, Boolean> isActiveFunction) {
        final Boolean isActive = isActiveFunction.apply(this);
        return AgentSummary.create(getId(), getNodeId(), getNodeDetails().toSummary(),
                getLastSeen(), getAgentVersion(), isActive != null && isActive);
    }

    @Override
    @JsonIgnore
    public String getOperatingSystem() {
        return getNodeDetails().operatingSystem();
    }

    @JsonProperty("last_seen")
    @Override
    public abstract DateTime getLastSeen();

    @JsonCreator
    public static AgentImpl create(@JsonProperty("_id") String objectId,
                                   @JsonProperty("id") String id,
                                   @JsonProperty("node_id") String nodeId,
                                   @JsonProperty("node_details") AgentNodeDetails agentNodeDetails,
                                   @JsonProperty("agent_version") String agentVersion,
                                   @JsonProperty("last_seen") DateTime lastSeen) {
        return new AutoValue_AgentImpl(id, nodeId, agentNodeDetails, agentVersion, lastSeen);
    }

    public static AgentImpl create(String agentId, String nodeId, String agentVersion, AgentNodeDetails agentNodeDetails, DateTime lastSeen) {
        return new AutoValue_AgentImpl(agentId, nodeId, agentNodeDetails, agentVersion, lastSeen);
    }
}
