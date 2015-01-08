/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Api(value = "System/Cluster", description = "Node discovery")
@Path("/system/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterResource.class);

    private final NodeService nodeService;
    private final NodeId nodeId;

    @Inject
    public ClusterResource(NodeService nodeService,
                           NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
    }

    @GET
    @Timed
    @Path("/nodes")
    @ApiOperation(value = "List all active nodes in this cluster.")
    public Map<String, Object> nodes() {
        final List<Map<String, Object>> nodeList = Lists.newArrayList();

        final Map<String, Node> nodes = nodeService.allActive(Node.Type.SERVER);
        for (Map.Entry<String, Node> e : nodes.entrySet()) {
            nodeList.add(nodeSummary(e.getValue()));
        }

        return ImmutableMap.of(
                "total", nodes.size(),
                "nodes", nodeList);
    }

    @GET
    @Timed
    @Path("/node")
    @ApiOperation(value = "Information about this node.",
            notes = "This is returning information of this node in context to its state in the cluster. " +
                    "Use the system API of the node itself to get system information.")
    public Node node() throws NodeNotFoundException {
        return nodeService.byNodeId(nodeId);
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
    public Node node(@ApiParam(name = "nodeId", required = true) @PathParam("nodeId") String nodeId) throws NodeNotFoundException {
        if (nodeId == null || nodeId.isEmpty()) {
            LOG.error("Missing nodeId. Returning HTTP 400.");
            throw new BadRequestException();
        }

        return nodeService.byNodeId(nodeId);
    }

    private Map<String, Object> nodeSummary(Node node) {
        Map<String, Object> m = Maps.newHashMap();

        m.put("node_id", node.getNodeId());
        m.put("type", node.getType().toString().toLowerCase());
        m.put("is_master", node.isMaster());
        m.put("transport_address", node.getTransportAddress());
        m.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

        // Only meant to be used for representation. Not a real ID.
        m.put("short_node_id", node.getShortNodeId());

        return m;
    }
}
