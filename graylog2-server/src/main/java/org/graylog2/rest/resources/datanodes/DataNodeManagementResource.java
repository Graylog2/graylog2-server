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

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.UserContext;
import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.DataNodeService;
import org.graylog2.rest.bulk.AuditParams;
import org.graylog2.rest.bulk.BulkExecutor;
import org.graylog2.rest.bulk.SequentialBulkExecutor;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

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
    private final NodeService<DataNodeDto> nodeService;
    private final CertRenewalService certRenewalService;
    private final BulkExecutor<DataNodeDto, UserContext> bulkRemovalExecutor;

    @Inject
    protected DataNodeManagementResource(DataNodeService dataNodeService,
                                         NodeService<DataNodeDto> nodeService,
                                         CertRenewalService certRenewalService, AuditEventSender auditEventSender, ObjectMapper objectMapper) {
        this.dataNodeService = dataNodeService;
        this.nodeService = nodeService;
        this.certRenewalService = certRenewalService;
        bulkRemovalExecutor = new SequentialBulkExecutor<>(this::removeNode, auditEventSender, objectMapper);
    }

    @GET
    @Path("{nodeId}")
    @ApiOperation("Get data node information")
    public DataNodeDto getDataNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            return certRenewalService.addProvisioningInformation(nodeService.byNodeId(nodeId));
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @DELETE
    @Path("{nodeId}")
    @ApiOperation("Remove node from cluster")
    @AuditEvent(type = DATANODE_REMOVE)
    @RequiresPermissions(RestPermissions.DATANODE_REMOVE)
    public DataNodeDto removeNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId,
                                  @Context UserContext userContext) {
        try {
            return dataNodeService.removeNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("/bulk_remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation(value = "Remove multiple nodes from the cluster", response = BulkOperationResponse.class)
    @NoAuditEvent("Audit events triggered manually")
    public Response bulkRemove(@ApiParam(name = "Entities to remove", required = true) final BulkOperationRequest bulkOperationRequest,
                               @Context UserContext userContext) {

        final BulkOperationResponse response = bulkRemovalExecutor.executeBulkOperation(bulkOperationRequest,
                userContext,
                new AuditParams(DATANODE_REMOVE, "nodeId", DataNodeDto.class));

        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @POST
    @Path("{nodeId}/reset")
    @ApiOperation("Reset a removed node to rejoin the cluster")
    @AuditEvent(type = DATANODE_RESET)
    @RequiresPermissions(RestPermissions.DATANODE_RESET)
    public DataNodeDto resetNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            return dataNodeService.resetNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("{nodeId}/stop")
    @ApiOperation("Stop the OpenSearch process of a data node")
    @AuditEvent(type = DATANODE_STOP)
    @RequiresPermissions(RestPermissions.DATANODE_STOP)
    public DataNodeDto stopNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            return dataNodeService.stopNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }

    @POST
    @Path("{nodeId}/start")
    @ApiOperation("Start the OpenSearch process of a data node")
    @AuditEvent(type = DATANODE_START)
    @RequiresPermissions(RestPermissions.DATANODE_START)
    public DataNodeDto startNode(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) {
        try {
            return dataNodeService.startNode(nodeId);
        } catch (NodeNotFoundException e) {
            throw new NotFoundException("Node " + nodeId + " not found");
        }
    }
}
