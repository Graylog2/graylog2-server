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
package org.graylog2.rest.models.agent.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.agent.AgentNodeDetailsSummary;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@AutoValue
@JsonAutoDetect
public abstract class AgentRegistrationRequest {
    @JsonProperty("node_id")
    @NotNull
    @Size(min = 1)
    public abstract String nodeId();

    @JsonProperty("node_details")
    public abstract AgentNodeDetailsSummary nodeDetails();

    @JsonCreator
    public static AgentRegistrationRequest create(@JsonProperty("node_id") String nodeId,
                                                  @JsonProperty("node_details") @Valid AgentNodeDetailsSummary nodeDetails) {
        return new AutoValue_AgentRegistrationRequest(nodeId, nodeDetails);
    }
}
