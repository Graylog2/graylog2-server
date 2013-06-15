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
package org.graylog2.rest.resources.system.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.RestResource;
import org.graylog2.systemjobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/indexer/cluster")
public class ClusterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    ResourceConfig rc;
    @GET
    @Path("/name")
    @Produces(MediaType.APPLICATION_JSON)
    public String name(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("name", core.getIndexer().cluster().getName());

        return json(result, prettyPrint);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public String health(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Integer> shards = Maps.newHashMap();
        shards.put("active", core.getIndexer().cluster().getActiveShards());
        shards.put("initializing", core.getIndexer().cluster().getInitializingShards());
        shards.put("relocating", core.getIndexer().cluster().getRelocatingShards());
        shards.put("unassigned", core.getIndexer().cluster().getUnassignedShards());

        Map<String, Object> health = Maps.newHashMap();
        health.put("status", core.getIndexer().cluster().getHealth().toString().toLowerCase());
        health.put("shards", shards);

        return json(health, prettyPrint);
    }

}
