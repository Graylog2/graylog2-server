package org.graylog2.rest.resources.system.agent;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.rest.models.agent.requests.AgentRegistrationRequest;
import org.graylog2.shared.rest.resources.RestResource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "System/Agents/Registration", description = "Registration resource for graylog agent nodes.")
@Path("/system/agents/register")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentRegistrationResource extends RestResource {
    @POST
    @Timed
    @ApiOperation(value = "Register - create/update an agent registration",
            notes = "This is a stateless method which upserts and agent registration")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The supplied request is not valid.")
    })
    public Response register(@ApiParam(name = "JSON body", required = true)
                             @Valid @NotNull AgentRegistrationRequest request) {
        return Response.accepted().build();
    }
}
