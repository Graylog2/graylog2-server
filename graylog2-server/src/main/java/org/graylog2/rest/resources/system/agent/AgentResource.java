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
package org.graylog2.rest.resources.system.agent;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.primitives.Ints;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.agents.Agent;
import org.graylog2.agents.AgentService;
import org.graylog2.agents.Agents;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.agent.responses.AgentList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "System/Agents", description = "Management of Graylog agents.")
@Path("/system/agents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@RequiresPermissions(RestPermissions.AGENTS_READ)
public class AgentResource extends RestResource {
    private final AgentService agentService;
    private final LostAgentFunction lostAgentFunction;

    public class LostAgentFunction implements Function<Agent, Boolean> {
        private final long timeOutInSeconds;

        @Inject
        public LostAgentFunction(long timeOutInSeconds) {
            this.timeOutInSeconds = timeOutInSeconds;
        }

        @Override
        public Boolean apply(Agent agent) {
            final DateTime threshold = DateTime.now().minusSeconds(Ints.checkedCast(timeOutInSeconds));
            return agent.getLastSeen().isAfter(threshold);
        }
    }

    @Inject
    public AgentResource(AgentService agentService, Configuration config) {
        this.agentService = agentService;
        this.lostAgentFunction = new LostAgentFunction(config.getAgentInactiveThreshold().toSeconds());
    }

    @GET
    @Timed
    @ApiOperation(value = "Lists all existing agent registrations")
    public AgentList list() {
        final List<Agent> agents = agentService.all();
        final List<AgentSummary> agentSummaries = Agents.toSummaryList(agents, lostAgentFunction);
        return AgentList.create(agentSummaries);
    }

    @GET
    @Timed
    @Path("/{agentId}")
    @ApiOperation(value = "Returns at most one agent summary for the specified agent id")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No agent with the specified id exists")
    })
    public AgentSummary get(@ApiParam(name = "agentId", required = true)
                            @PathParam("agentId") String agentId) throws NotFoundException {
        final Agent agent = agentService.findById(agentId);

        if (agent != null)
            return agent.toSummary(lostAgentFunction);
        else
            throw new NotFoundException("Agent <" + agentId + "> not found!");
    }
}
