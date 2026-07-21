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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
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
import org.graylog.scheduler.JobTriggerDto;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.datanode.restart.RollingRestartJobHandler;
import org.graylog2.datanode.restart.RollingRestartPreconditionsException;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Map;

import static org.graylog2.audit.AuditEventTypes.DATANODE_ABORT_RESTART;
import static org.graylog2.audit.AuditEventTypes.DATANODE_RESUME_RESTART;
import static org.graylog2.audit.AuditEventTypes.DATANODE_TRIGGER_RESTART;

@RequiresAuthentication
@Tag(name = "DataNode/Rolling Restart", description = "Endpoint for rolling restart of embedded OpenSearch instances in a data node cluster")
@Path("/datanodes/restart")
@Produces(MediaType.APPLICATION_JSON)
public class RollingRestartResource extends RestResource {

    private final RollingRestartJobHandler handler;

    @Inject
    public RollingRestartResource(RollingRestartJobHandler handler) {
        this.handler = handler;
    }

    @POST
    @Operation(summary = "Trigger rolling restart of embedded OpenSearch")
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    @AuditEvent(type = DATANODE_TRIGGER_RESTART)
    public Response start(@RequestBody(required = false) StartRequest request) {
        try {
            final String triggeredBy = String.valueOf(SecurityUtils.getSubject().getPrincipal());
            final JobTriggerDto trigger = handler.start(triggeredBy, request != null && request.force());
            return Response.status(Response.Status.CREATED).entity(trigger).build();
        } catch (RollingRestartPreconditionsException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("failed_checks", e.getFailedChecks()))
                    .build();
        } catch (IllegalStateException e) {
            // Raised when the start-lock is currently held by another concurrent /restart call.
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Operation(summary = "Get information on current rolling restart operation")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public Response current() {
        return Response.ok(handler.current().orElse(null)).build();
    }

    @GET
    @Path("/history")
    @Operation(summary = "Get history of rolling restart operations")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public List<JobTriggerDto> history(@QueryParam("limit") @DefaultValue("20") int limit) {
        return handler.history(Math.max(1, Math.min(limit, 200)));
    }

    @POST
    @Path("/abort")
    @Operation(summary = "Abort a rolling restart operation")
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    @AuditEvent(type = DATANODE_ABORT_RESTART)
    public JobTriggerDto abort() {
        try {
            return handler.abort();
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/resume")
    @Operation(summary = "Resume a rolling restart operation")
    @RequiresPermissions(RestPermissions.DATANODE_RESTART)
    @AuditEvent(type = DATANODE_RESUME_RESTART)
    public JobTriggerDto resume() {
        try {
            return handler.resume();
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record StartRequest(@JsonProperty("force") boolean force) {
    }
}
