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
import com.google.common.base.Strings;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.agents.Agent;
import org.graylog2.agents.AgentImpl;
import org.graylog2.agents.AgentService;
import org.graylog2.rest.models.agent.requests.AgentRegistrationRequest;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Strings.isNullOrEmpty;

@Api(value = "System/Agents/Registration", description = "Registration resource for graylog agent nodes.")
@Path("/system/agents/register")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentRegistrationResource extends RestResource {

    private final AgentService agentService;

    @Inject
    public AgentRegistrationResource(AgentService agentService) {
        this.agentService = agentService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Register - create/update an agent registration",
            notes = "This is a stateless method which upserts and agent registration")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The supplied request is not valid.")
    })
    public Response register(@ApiParam(name = "JSON body", required = true)
                             @Valid @NotNull AgentRegistrationRequest request) {
        final Agent agent = agentService.fromRequest(request);

        agentService.save(agent);

        return Response.accepted().build();
    }
}
