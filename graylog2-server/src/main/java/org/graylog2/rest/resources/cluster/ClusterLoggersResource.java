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
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.RestTools;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import org.graylog2.rest.resources.system.logs.RemoteLoggersResource;
import org.graylog2.shared.rest.HideOnCloud;
import org.graylog2.shared.rest.resources.ProxiedResource;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@RequiresAuthentication
@Tag(name = "Cluster/System/Loggers", description = "Cluster-wide access to internal Graylog loggers")
@Path("/cluster/system/loggers")
public class ClusterLoggersResource extends ProxiedResource {
    @Inject
    public ClusterLoggersResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Timed
    @Operation(summary = "List all loggers of all nodes and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<LoggersSummary>> loggers() {
        return stripCallResult(requestOnAllNodes(RemoteLoggersResource.class, RemoteLoggersResource::loggers));
    }

    @GET
    @Timed
    @Path("/subsystems")
    @Operation(summary = "List all logger subsystems and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<SubsystemSummary>> subsystems() {
        return stripCallResult(requestOnAllNodes(RemoteLoggersResource.class, RemoteLoggersResource::subsystems));
    }

    @PUT
    @Timed
    @Path("/{nodeId}/subsystems/{subsystem}/level/{level}")
    @Operation(summary = "Set the loglevel of a whole subsystem",
                  description = "Provided level is falling back to DEBUG if it does not exist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "404", description = "No such subsystem.")
    })
    @NoAuditEvent("proxy resource, audit event will be emitted on target nodes")
    public void setSubsystemLoggerLevel(
            @Parameter(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId,
            @Parameter(name = "subsystem", required = true) @PathParam("subsystem") @NotEmpty String subsystemTitle,
            @Parameter(name = "level", required = true) @PathParam("level") @NotEmpty String level) throws NodeNotFoundException, IOException {
        final Node node = this.nodeService.byNodeId(nodeId);
        final RemoteLoggersResource remoteLoggersResource = this.remoteInterfaceProvider.get(node, getAuthenticationToken(), RemoteLoggersResource.class);

        remoteLoggersResource.setSubsystemLoggerLevel(subsystemTitle, level).execute();
    }

    @PUT
    @Timed
    @Path("/{loggerName}/level/{level}")
    @Operation(summary = "Set the loglevel of a single logger",
                  description = "Provided level is falling back to DEBUG if it does not exist")
    @NoAuditEvent("proxy resource, audit event will be emitted on target nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CallResult<Void>> setClusterSingleLoggerLevel(
            @Parameter(name = "loggerName", required = true) @PathParam("loggerName") @NotEmpty String loggerName,
            @Parameter(name = "level", required = true) @PathParam("level") @NotEmpty String level) {
        return requestOnAllNodes(RemoteLoggersResource.class, client -> client.setSingleLoggerLevel(loggerName, level));
    }

    @GET
    @Path("messages/recent/{nodeId}")
    @Operation(summary = "Get recent internal log messages from a specific node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "Memory appender is disabled."),
            @ApiResponse(responseCode = "500", description = "Memory appender is broken.")
    })
    @Produces(MediaType.TEXT_PLAIN)
    @HideOnCloud
    public Response messages(@Parameter(name = "nodeId", description = "The nodeId to get logs from") @PathParam("nodeId") @NotEmpty String nodeId,
                             @Parameter(name = "limit", description = "How many log messages should be returned. 0 returns all existing messages." +
                                     "The limit can be rounded up to the next batch size and thus return slightly more logs than requested.")
                             @QueryParam("limit") @DefaultValue("1000") @Min(0L) int limit) throws IOException {

        var nodeResponse = doNodeApiCall(nodeId, RemoteLoggersResource.class, c -> c.messages(limit), Function.identity(), null);
        return RestTools.streamResponse(nodeResponse, MediaType.TEXT_PLAIN, null);
    }
}
