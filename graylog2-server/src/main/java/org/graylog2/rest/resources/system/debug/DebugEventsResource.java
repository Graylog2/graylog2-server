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
package org.graylog2.rest.resources.system.debug;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.debug.DebugEvent;
import org.graylog2.system.debug.DebugEventHolder;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@RequiresAuthentication
@Tag(name = "System/Debug/Events", description = "For debugging local and cluster events.")
@Path("/system/debug/events")
@Produces(MediaType.APPLICATION_JSON)
public class DebugEventsResource extends RestResource {
    private final NodeId nodeId;
    private final EventBus serverEventBus;
    private final EventBus clusterEventBus;

    @Inject
    public DebugEventsResource(NodeId nodeId,
                               EventBus serverEventBus,
                               ClusterEventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.serverEventBus = checkNotNull(serverEventBus);
        this.clusterEventBus = checkNotNull(clusterEventBus);
    }

    @Timed
    @POST
    @Path("/cluster")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Create and send a cluster debug event.")
    @NoAuditEvent("only used to create a debug event")
    public void generateClusterDebugEvent(@Parameter(name = "text") @Nullable String text) {
        clusterEventBus.post(DebugEvent.create(nodeId.getNodeId(), isNullOrEmpty(text) ? "Cluster Test" : text));
    }

    @Timed
    @POST
    @Path("/local")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Create and send a local debug event.")
    @NoAuditEvent("only used to create a debug event")
    public void generateDebugEvent(@Parameter(name = "text") @Nullable String text) {
        serverEventBus.post(DebugEvent.create(nodeId.getNodeId(), isNullOrEmpty(text) ? "Local Test" : text));
    }

    @Timed
    @GET
    @Path("/cluster")
    @Operation(summary = "Show last received cluster debug event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Show last received cluster debug event. retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DebugEvent.class)))
    })
    public DebugEvent showLastClusterDebugEvent() {
        return DebugEventHolder.getClusterDebugEvent();
    }

    @Timed
    @GET
    @Path("/local")
    @Operation(summary = "Show last received local debug event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Show last received local debug event. retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DebugEvent.class)))
    })
    public DebugEvent showLastDebugEvent() {
        return DebugEventHolder.getLocalDebugEvent();
    }
}
