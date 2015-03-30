package org.graylog2.rest.resources.system.agent;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.rest.models.agent.AgentNodeDetails;
import org.graylog2.rest.models.agent.responses.AgentList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Api(value = "System/Agents", description = "Management of graylog agents.")
@Path("/system/agents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource extends RestResource {
    @GET
    @Timed
    @ApiOperation(value = "List - lists all existing agent registrations")
    public AgentList list() {
        final List<AgentSummary> agents = Collections.emptyList();
        return AgentList.create(agents);
    }

    @GET
    @Timed
    @Path("/{agentId}")
    @ApiOperation(value = "Get - Returns at most one agent summary for the specified agent id")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No agent with the specified id exists")
    })
    public AgentSummary get(@ApiParam(name = "agentId", required = true)
                            @PathParam("agentId") String agentId) {
        return AgentSummary.create("foo", "bar", AgentNodeDetails.create("DummyOs"), DateTime.now());
    }
}
