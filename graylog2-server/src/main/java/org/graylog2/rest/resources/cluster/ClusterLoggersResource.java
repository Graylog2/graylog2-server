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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import okhttp3.ResponseBody;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import org.graylog2.rest.resources.system.logs.RemoteLoggersResource;
import org.graylog2.shared.rest.HideOnCloud;
import org.graylog2.shared.rest.resources.ProxiedResource;

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
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@RequiresAuthentication
@Api(value = "Cluster/System/Loggers", description = "Cluster-wide access to internal Graylog loggers")
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
    @ApiOperation(value = "List all loggers of all nodes and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<LoggersSummary>> loggers() {
        return stripCallResult(requestOnAllNodes(RemoteLoggersResource.class, RemoteLoggersResource::loggers));
    }

    @GET
    @Timed
    @Path("/subsystems")
    @ApiOperation(value = "List all logger subsystems and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<SubsystemSummary>> subsystems() {
        return stripCallResult(requestOnAllNodes(RemoteLoggersResource.class, RemoteLoggersResource::subsystems));
    }

    @PUT
    @Timed
    @Path("/{nodeId}/subsystems/{subsystem}/level/{level}")
    @ApiOperation(value = "Set the loglevel of a whole subsystem",
                  notes = "Provided level is falling back to DEBUG if it does not exist")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such subsystem.")
    })
    @NoAuditEvent("proxy resource, audit event will be emitted on target nodes")
    public void setSubsystemLoggerLevel(
            @ApiParam(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId,
            @ApiParam(name = "subsystem", required = true) @PathParam("subsystem") @NotEmpty String subsystemTitle,
            @ApiParam(name = "level", required = true) @PathParam("level") @NotEmpty String level) throws NodeNotFoundException, IOException {
        final Node node = this.nodeService.byNodeId(nodeId);
        final RemoteLoggersResource remoteLoggersResource = this.remoteInterfaceProvider.get(node, getAuthenticationToken(), RemoteLoggersResource.class);

        remoteLoggersResource.setSubsystemLoggerLevel(subsystemTitle, level).execute();
    }

    @PUT
    @Timed
    @Path("/{loggerName}/level/{level}")
    @ApiOperation(value = "Set the loglevel of a single logger",
                  notes = "Provided level is falling back to DEBUG if it does not exist")
    @NoAuditEvent("proxy resource, audit event will be emitted on target nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CallResult<Void>> setClusterSingleLoggerLevel(
            @ApiParam(name = "loggerName", required = true) @PathParam("loggerName") @NotEmpty String loggerName,
            @ApiParam(name = "level", required = true) @PathParam("level") @NotEmpty String level) {
        return requestOnAllNodes(RemoteLoggersResource.class, client -> client.setSingleLoggerLevel(loggerName, level));
    }

    @GET
    @Path("messages/recent/{nodeId}")
    @ApiOperation(value = "Get recent internal log messages from a specific node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Memory appender is disabled."),
            @ApiResponse(code = 500, message = "Memory appender is broken.")
    })
    @Produces(MediaType.TEXT_PLAIN)
    @HideOnCloud
    public Response messages(@ApiParam(name = "nodeId", value = "The nodeId to get logs from") @PathParam("nodeId") @NotEmpty String nodeId,
                             @ApiParam(name = "limit", value = "How many log messages should be returned. 0 returns all existing messages." +
                                     "The limit can be rounded up to the next batch size and thus return slightly more logs than requested.",
                                       defaultValue = "1000", allowableValues = "range[0, infinity]")
                             @QueryParam("limit") @DefaultValue("1000") @Min(0L) int limit) throws IOException {

        var nodeResponse = doNodeApiCall(nodeId, RemoteLoggersResource.class, c -> c.messages(limit), Function.identity(), null);

        if (nodeResponse.isSuccess()) {
            // we cannot use try-with because the ResponseBody needs to stream the output
            ResponseBody responseBody = nodeResponse.entity().orElseThrow();

            try {
                StreamingOutput streamingOutput = output -> {
                    try {
                        responseBody.byteStream().transferTo(output);
                    } catch (Exception e) {
                        responseBody.close(); // avoid leaking connections on errors
                    }
                };
                var mediaType = MediaType.valueOf(MediaType.TEXT_PLAIN);
                Response.ResponseBuilder response = Response.ok(streamingOutput, mediaType);
                return response.build();
            } catch (Exception e) {
                responseBody.close();
            }
        }
        return Response.status(nodeResponse.code()).entity(nodeResponse.body()).build();
    }
}
