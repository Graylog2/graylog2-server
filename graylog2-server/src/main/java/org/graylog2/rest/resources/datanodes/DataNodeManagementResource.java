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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.datanode.DataNodeService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.graylog2.audit.AuditEventTypes.DATANODE_REMOVE;
import static org.graylog2.audit.AuditEventTypes.DATANODE_RESET;
import static org.graylog2.audit.AuditEventTypes.DATANODE_START;
import static org.graylog2.audit.AuditEventTypes.DATANODE_STOP;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Datanode", description = "Data Node", tags = {CLOUD_VISIBLE})
@Path("/datanode/")
@Produces(MediaType.APPLICATION_JSON)
public class DataNodeManagementResource extends RestResource {

    private final DataNodeService dataNodeService;

    @Inject
    protected DataNodeManagementResource(DataNodeService dataNodeService) {
        this.dataNodeService = dataNodeService;
    }

    @DELETE
    @Path("{nodeId}")
    @ApiOperation("Remove node from cluster")
    @AuditEvent(type = DATANODE_REMOVE)
    @RequiresPermissions(RestPermissions.DATANODE_REMOVE)
    public void removeNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            dataNodeService.removeNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("{nodeId}/reset")
    @ApiOperation("Reset a removed node to rejoin the cluster")
    @AuditEvent(type = DATANODE_RESET)
    @RequiresPermissions(RestPermissions.DATANODE_RESET)
    public void resetNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            dataNodeService.resetNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("{nodeId}/stop")
    @ApiOperation("Stop the OpenSearch process of a data node")
    @AuditEvent(type = DATANODE_STOP)
    @RequiresPermissions(RestPermissions.DATANODE_STOP)
    public void stopNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            dataNodeService.stopNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("{nodeId}/start")
    @ApiOperation("Start the OpenSearch process of a data node")
    @AuditEvent(type = DATANODE_START)
    @RequiresPermissions(RestPermissions.DATANODE_START)
    public void startNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            dataNodeService.startNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }
}
