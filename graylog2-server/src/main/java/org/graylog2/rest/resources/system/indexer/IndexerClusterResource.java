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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.cluster.Cluster;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Indexer/Cluster", description = "Indexer cluster information")
@Path("/system/indexer/cluster")
public class IndexerClusterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerClusterResource.class);

    @Inject
    private Cluster cluster;

    @GET @Timed
    @Path("/name")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @ApiOperation(value = "Get the cluster name")
    @Produces(MediaType.APPLICATION_JSON)
    public String clusterName() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("name", cluster.getName());

        return json(result);
    }

    @GET @Timed
    @Path("/health")
    @ApiOperation(value = "Get cluster and shard health overview")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public String clusterHealth() {
        Map<String, Integer> shards = Maps.newHashMap();
        shards.put("active", cluster.getActiveShards());
        shards.put("initializing", cluster.getInitializingShards());
        shards.put("relocating", cluster.getRelocatingShards());
        shards.put("unassigned", cluster.getUnassignedShards());

        Map<String, Object> health = Maps.newHashMap();
        health.put("status", cluster.getHealth().toString().toLowerCase());
        health.put("shards", shards);

        return json(health);
    }

}
