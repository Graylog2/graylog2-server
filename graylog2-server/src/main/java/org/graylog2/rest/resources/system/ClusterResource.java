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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.eaio.uuid.UUID;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.cluster.responses.NodeSummary;
import org.graylog2.rest.models.system.cluster.responses.NodeSummaryList;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Api(value = "System/Cluster", description = "Node discovery")
@RequiresAuthentication
@Path("/system/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterResource extends RestResource {
    private final NodeService nodeService;
    private final NodeId nodeId;
    private final ClusterId clusterId;

    @Inject
    public ClusterResource(NodeService nodeService,
                           ClusterConfigService clusterConfigService,
                           NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
    }

    @GET
    @Timed
    @Path("/nodes")
    @ApiOperation(value = "List all active nodes in this cluster.")
    public NodeSummaryList nodes() {
        final Map<String, Node> nodes = nodeService.allActive(Node.Type.SERVER);
        final List<NodeSummary> nodeList = new ArrayList<>(nodes.size());
        for (Node node : nodes.values()) {
            nodeList.add(nodeSummary(node));
        }

        return NodeSummaryList.create(nodeList);
    }

    @GET
    @Timed
    @Path("/node")
    @ApiOperation(value = "Information about this node.",
            notes = "This is returning information of this node in context to its state in the cluster. " +
                    "Use the system API of the node itself to get system information.")
    public NodeSummary node() throws NodeNotFoundException {
        return nodeSummary(nodeService.byNodeId(nodeId));
    }

    @GET
    @Timed
    @Path("/nodes/{nodeId}")
    @ApiOperation(value = "Information about a node.",
            notes = "This is returning information of a node in context to its state in the cluster. " +
                    "Use the system API of the node itself to get system information.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Node not found.")
    })
    public NodeSummary node(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") @NotEmpty String nodeId) throws NodeNotFoundException {
        return nodeSummary(nodeService.byNodeId(nodeId));
    }

    private NodeSummary nodeSummary(Node node) {
        return NodeSummary.create(
                clusterId.clusterId(),
                node.getNodeId(),
                node.getType().toString().toLowerCase(Locale.ENGLISH),
                node.isMaster(),
                node.getTransportAddress(),
                Tools.getISO8601String(node.getLastSeen()),
                node.getShortNodeId(),
                node.getHostname()
        );
    }
}
