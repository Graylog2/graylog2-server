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
package org.graylog2.rest.resources.datanodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.datanode.restart.RollingRestartJob;
import org.graylog2.datanode.restart.RollingRestartPreconditionsException;
import org.graylog2.datanode.restart.RollingRestartService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Tag(name = "DataNode/Rolling Restart")
@Path("/datanodes/restart")
@Produces(MediaType.APPLICATION_JSON)
public class RollingRestartResource extends RestResource {

    private final RollingRestartService service;

    @Inject
    public RollingRestartResource(RollingRestartService service) {
        this.service = service;
    }

    @POST
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    public Response start(@NotNull StartRequest request) {
        try {
            final String triggeredBy = String.valueOf(SecurityUtils.getSubject().getPrincipal());
            final RollingRestartJob job = service.start(triggeredBy, request != null && request.force());
            return Response.status(Response.Status.CREATED).entity(job).build();
        } catch (RollingRestartPreconditionsException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("failed_checks", e.getFailedChecks()))
                    .build();
        }
    }

    @GET
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public Response current() {
        return Response.ok(service.currentJob().orElse(null)).build();
    }

    @GET
    @Path("/history")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public List<RollingRestartJob> history(@QueryParam("limit") @DefaultValue("20") int limit) {
        return service.history(Math.max(1, Math.min(limit, 200)));
    }

    @POST
    @Path("/abort")
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    public RollingRestartJob abort() {
        try {
            return service.abort();
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/resume")
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    public RollingRestartJob resume() {
        try {
            return service.resume();
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record StartRequest(@JsonProperty("force") boolean force) {
    }
}
