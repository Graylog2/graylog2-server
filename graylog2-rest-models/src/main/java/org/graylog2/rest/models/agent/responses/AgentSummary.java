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

    @JsonProperty("agent_version")
    public abstract String agentVersion();

    @JsonProperty
    public abstract boolean active();

    @JsonCreator
    public static AgentSummary create(@JsonProperty("id") String id,
                                      @JsonProperty("node_id") String nodeId,
                                      @JsonProperty("node_details") AgentNodeDetailsSummary nodeDetails,
                                      @JsonProperty("last_seen") DateTime lastSeen,
                                      @JsonProperty("agent_version") String agentVersion,
                                      @JsonProperty("active") boolean active) {
        return new AutoValue_AgentSummary(id, nodeId, nodeDetails, lastSeen, agentVersion, active);
    }
}
