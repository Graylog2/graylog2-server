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

import com.google.common.base.Function;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.joda.time.DateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

public interface Agent {
    @NotNull
    @Size(min = 1)
    String getId();

    @NotNull
    @Size(min = 1)
    String getNodeId();

    @NotNull
    String getAgentVersion();

    @NotNull
    @Past
    DateTime getLastSeen();

    @NotNull
    @Valid
    AgentNodeDetails getNodeDetails();

    AgentSummary toSummary(Function<Agent, Boolean> isActiveFunction);
}
