/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.cluster.Node;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/cluster/nodes")
public class NodesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(NodesResource.class);

    @Context
    ResourceConfig rc;

    @GET
    @Timed
    @Path("/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String node(@PathParam("nodeId") String nodeId, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (nodeId == null || nodeId.isEmpty()) {
            LOG.error("Missing nodeId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        Node node = Node.byNodeId(core, nodeId);

        if (node == null) {
            LOG.error("Node <{}> not found.", nodeId);
            throw new WebApplicationException(404);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("node_id", node.getNodeId());
        result.put("transport_address", node.getTransportAddress());
        result.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

        return json(result, prettyPrint);
    }

    @GET
    @Timed
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        List<Map<String, Object>> nodes = Lists.newArrayList();

        for (Node node : Node.allActive(core).values()) {
            Map<String, Object> nodeMap = Maps.newHashMap();
            nodeMap.put("node_id", node.getNodeId());
            nodeMap.put("transport_address", node.getTransportAddress());
            nodeMap.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

            nodes.add(nodeMap);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", nodes.size());
        result.put("nodes", nodes);

        return json(result, prettyPrint);
    }
}
