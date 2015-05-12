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
package org.graylog2.rest.resources.system.collector;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.collectors.Collector;
import org.graylog2.collectors.CollectorService;
import org.graylog2.rest.models.collector.requests.CollectorRegistrationRequest;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "System/Collectors/Registration", description = "Registration resource for Graylog Collector nodes.")
@Path("/system/collectors/{collectorId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CollectorRegistrationResource extends RestResource {

    private final CollectorService collectorService;

    @Inject
    public CollectorRegistrationResource(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @PUT
    @Timed
    @ApiOperation(value = "Create/update an collector registration",
            notes = "This is a stateless method which upserts a collector registration")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The supplied request is not valid.")
    })
    public Response register(@ApiParam(name = "collectorId", value = "The collector id this collector is registering as.", required = true)
                             @PathParam("collectorId") String collectorId,
                             @ApiParam(name = "JSON body", required = true)
                             @Valid @NotNull CollectorRegistrationRequest request,
                             @HeaderParam(value = "X-Graylog-Collector-Version") String collectorVersion) {
        final Collector collector = collectorService.fromRequest(collectorId, request, collectorVersion);

        collectorService.save(collector);

        return Response.accepted().build();
    }
}
