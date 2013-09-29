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
package selenium.serverstub.rest.resources.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import selenium.serverstub.ServerStub;
import selenium.serverstub.rest.resources.RestResource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system")
public class SystemResource extends RestResource {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    com.sun.jersey.api.core.ResourceConfig rc;

    @GET
    @Path("/notifications")
    public String notifications() {
        ServerStub core = (ServerStub) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", core.systemNotifications.size());
        result.put("notifications", core.systemNotifications);

        return json(result);
    }

    @GET
    @Path("/messages")
    public String messages() {
        ServerStub core = (ServerStub) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("messages", core.systemMessages);

        return json(result);
    }

    @GET
    @Path("/jobs")
    public String jobs() {
        ServerStub core = (ServerStub) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("jobs", core.systemJobs);

        return json(result);
    }

    @GET
    @Path("/indexer/cluster/health")
    public String esHealth() {
        ServerStub core = (ServerStub) rc.getProperty("core");

        Map<String, Integer> shards = Maps.newHashMap();
        shards.put("active", 4);
        shards.put("initializing", 0);
        shards.put("relocating", 0);
        shards.put("unassigned", 0);

        Map<String, Object> result = Maps.newHashMap();
        result.put("status", "green");
        result.put("shards", shards);

        return json(result);
    }

}
