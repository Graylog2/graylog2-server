package org.graylog2.rest.resources.system.agent;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.agents.Agent;
import org.graylog2.agents.AgentService;
import org.graylog2.agents.Agents;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.agent.responses.AgentList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "System/Agents", description = "Management of graylog agents.")
@Path("/system/agents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource extends RestResource {
    private final AgentService agentService;

    @Inject
    public AgentResource(AgentService agentService) {
        this.agentService = agentService;
    }

    @GET
    @Timed
    @ApiOperation(value = "lists all existing agent registrations")
    public AgentList list() {
        final List<Agent> agents = agentService.all();
        final List<AgentSummary> agentSummaries = Agents.toSummaryList(agents);
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
            return agent.toSummary();
        else
            throw new NotFoundException("Agent <" + agentId + "> not found!");
    }
}
