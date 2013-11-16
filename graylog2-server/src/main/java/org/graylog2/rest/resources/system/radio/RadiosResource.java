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
package org.graylog2.rest.resources.system.radio;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.cluster.Node;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.radio.requests.PingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.ok;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "System/Radios", description = "Management of graylog2-radio nodes.")
@Path("/system/radios")
public class RadiosResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RadiosResource.class);

    @GET @Timed
    @ApiOperation(value = "List all active radios in this cluster.")
    public String nodes() {
        List<Map<String, Object>> nodeList = Lists.newArrayList();
        Map<String, Node> nodes = Node.allActive(core, Node.Type.RADIO);

        for(Map.Entry<String, Node> e : nodes.entrySet()) {
            nodeList.add(radioSummary(e.getValue()));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", nodes.size());
        result.put("radios", nodeList);

        return json(result);
    }

    @PUT @Timed
    @ApiOperation(value = "Ping - Accepts pings of graylog2-radio nodes.",
            notes = "Every graylog2-radio node is regularly pinging to announce that it is active.")
    @Path("/{radioId}/ping")
    public Response ping(@ApiParam(title = "JSON body", required = true) String body,
                         @ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId) {
        PingRequest pr;

        try {
            pr = objectMapper.readValue(body, PingRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        LOG.debug("Ping from graylog2-radio node [{}].", radioId);

        Node node = Node.byNodeId(core, radioId);

        if (node != null) {
            node.markAsAlive(false, pr.restTransportAddress);
            LOG.debug("Updated state of graylog2-radio node [{}].", radioId);
        } else {
            LOG.debug("There is no registered (or only outdated) graylog2-radio node [{}]. Registering.", radioId);
            Node.registerRadio(core, radioId, pr.restTransportAddress);
        }

        return ok().build();
    }

    private Map<String, Object> radioSummary(Node node) {
        Map<String, Object> m  = Maps.newHashMap();

        m.put("id", node.getNodeId());
        m.put("type", node.getType().toString().toLowerCase());
        m.put("transport_address", node.getTransportAddress());
        m.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

        // Only meant to be used for representation. Not a real ID.
        m.put("short_node_id", node.getShortNodeId());

        return m;
    }

}
