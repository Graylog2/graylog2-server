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
package org.graylog.datanode.rest;

import org.graylog.datanode.rest.config.OnlyInSecuredNode;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;

import jakarta.inject.Inject;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/management")
@Produces(MediaType.APPLICATION_JSON)
public class ManagementController {

    private final ClusterEventBus clusterEventBus;
    private final NodeId nodeId;


    @Inject
    public ManagementController(ClusterEventBus clusterEventBus, NodeId nodeId) {
        this.clusterEventBus = clusterEventBus;
        this.nodeId = nodeId;
    }

    @DELETE
    @OnlyInSecuredNode
    public void remove() {
        postEvent(DataNodeLifecycleTrigger.REMOVE);
    }

    @POST
    @Path("/start")
    @OnlyInSecuredNode
    public void start() {
        postEvent(DataNodeLifecycleTrigger.START);
    }

    @POST
    @Path("/stop")
    @OnlyInSecuredNode
    public void stop() {
        postEvent(DataNodeLifecycleTrigger.STOP);
    }

    private void postEvent(DataNodeLifecycleTrigger trigger) {
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(nodeId.getNodeId(), trigger);
        clusterEventBus.post(e);
    }

}
