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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.debug.DebugEvent;
import org.graylog2.system.debug.DebugEventHolder;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@RequiresAuthentication
@Api(value = "System/Debug/Events", description = "For debugging local and cluster events.")
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
    @ApiOperation(value = "Create and send a cluster debug event.")
    @NoAuditEvent("only used to create a debug event")
    public void generateClusterDebugEvent(@ApiParam(name = "text", defaultValue = "Cluster Test") @Nullable String text) {
        clusterEventBus.post(DebugEvent.create(nodeId.toString(), isNullOrEmpty(text) ? "Cluster Test" : text));
    }

    @Timed
    @POST
    @Path("/local")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Create and send a local debug event.")
    @NoAuditEvent("only used to create a debug event")
    public void generateDebugEvent(@ApiParam(name = "text", defaultValue = "Local Test") @Nullable String text) {
        serverEventBus.post(DebugEvent.create(nodeId.toString(), isNullOrEmpty(text) ? "Local Test" : text));
    }

    @Timed
    @GET
    @Path("/cluster")
    @ApiOperation(value = "Show last received cluster debug event.", response = DebugEvent.class)
    public DebugEvent showLastClusterDebugEvent() {
        return DebugEventHolder.getClusterDebugEvent();
    }

    @Timed
    @GET
    @Path("/local")
    @ApiOperation(value = "Show last received local debug event.", response = DebugEvent.class)
    public DebugEvent showLastDebugEvent() {
        return DebugEventHolder.getLocalDebugEvent();
    }
}
