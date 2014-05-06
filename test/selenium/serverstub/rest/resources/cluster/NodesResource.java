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
package selenium.serverstub.rest.resources.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.DateTools;
import org.joda.time.format.ISODateTimeFormat;
import selenium.serverstub.ServerStub;
import selenium.serverstub.rest.resources.RestResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/cluster/nodes")
public class NodesResource extends RestResource {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    com.sun.jersey.api.core.ResourceConfig rc;

    @GET
    @Path("/")
    public String notifications() {
        ServerStub core = (ServerStub) rc.getProperty("core");

        List<Map<String, Object>> nodes = Lists.newArrayList();

        Map<String, Object> nodeMap = Maps.newHashMap();
        nodeMap.put("node_id", core.nodeId);
        nodeMap.put("transport_address", core.transportAddress);
        nodeMap.put("last_seen", ISODateTimeFormat.dateTime().print(DateTools.nowInUTC()));
        nodes.add(nodeMap);

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", nodes.size());
        result.put("nodes", nodes);

        return json(result);
    }
}
