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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.filters.StreamOutputFilterService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Stream/Outputs/Filters", description = "Manage stream output filter rules", tags = {CLOUD_VISIBLE})
@Path("/streams/{streamId}/outputs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StreamOutputFiltersResource extends RestResource {
    private final StreamOutputFilterService filterService;

    @Inject
    public StreamOutputFiltersResource(StreamOutputFilterService filterService) {
        this.filterService = filterService;
    }

    @GET
    @Path("/filters")
    @ApiOperation(value = "Get available filter rules for stream")
    public Response getPaginatedFilters(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        // TODO: Check for each filter instance!
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_READ, streamId);

        return Response.ok().build();
    }

    @GET
    @Path("/target/{targetId}/filters")
    @ApiOperation(value = "Get available filter rules for stream")
    public Response getPaginatedFiltersForTarget(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                                 @ApiParam(name = "targetId", required = true) @PathParam("targetId") @NotBlank String targetId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        // TODO: Check for each filter instance!
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_READ, streamId);

        return Response.ok().build();
    }

    @GET
    @Path("/filters/{filterId}")
    @ApiOperation(value = "Get filter rule for given ID")
    public Response getFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                              @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_READ, filterId);

        return Response.ok().build();
    }

    @POST
    @Path("/filters")
    @ApiOperation(value = "Create new filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_OUTPUT_FILTER_CREATE)
    public Response createFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "JSON body", required = true) JsonNode body) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_CREATE);

        return Response.ok().build();
    }

    @PUT
    @Path("/filters/{filterId}")
    @ApiOperation(value = "Update filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_OUTPUT_FILTER_UPDATE)
    public Response updateFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_EDIT, filterId);

        return Response.ok().build();
    }

    @DELETE
    @Path("/filters/{filterId}")
    @ApiOperation(value = "Delete filter rule")
    @AuditEvent(type = AuditEventTypes.STREAM_OUTPUT_FILTER_DELETE)
    public Response deleteFilter(@ApiParam(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
                                 @ApiParam(name = "filterId", required = true) @PathParam("filterId") @NotBlank String filterId) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkPermission(RestPermissions.STREAM_OUTPUT_FILTERS_DELETE, filterId);

        return Response.ok().build();
    }
}
