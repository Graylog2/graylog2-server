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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.cluster.Node;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/cluster")
public class ClusterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterResource.class);

    @GET @Timed
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public String nodes() {
        List<Map<String, Object>> nodeList = Lists.newArrayList();
        Map<String, Node> nodes = Node.allActive(core);

        for(Map.Entry<String, Node> e : nodes.entrySet()) {
            Node node = e.getValue();
            Map<String, Object> nodeMap = Maps.newHashMap();

            nodeMap.put("id", node.getNodeId());
            nodeMap.put("is_master", node.isMaster());
            nodeMap.put("transport_address", node.getTransportAddress());
            nodeMap.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

            nodeList.add(nodeMap);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", nodes.size());
        result.put("nodes", nodeList);

        return json(result);
    }


}
