/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.streams.outputs.filters;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Stream/Outputs/Filters/Builder", description = "Stream output filter builder", tags = {CLOUD_VISIBLE})
@Path("/streams/outputs/filters/builder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StreamOutputFilterBuilderResource extends RestResource {
    @Inject
    public StreamOutputFilterBuilderResource() {
    }

    @GET
    @Path("/conditions")
    @ApiOperation(value = "Get available filter rule conditions")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUT_FILTERS_READ)
    public Response getConditions() {
        return Response.ok().build();
    }

    @POST
    @Path("/simulate")
    @ApiOperation(value = "Run the simulator for the given rule and message")
    @NoAuditEvent("No data changes. Only used to simulate a filter rule.")
    @RequiresPermissions(RestPermissions.STREAM_OUTPUT_FILTERS_READ)
    public Response simulateRule() {
        return Response.ok().build();
    }
}
