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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.indexer.responses.ClusterHealth;
import org.graylog2.rest.resources.system.indexer.responses.ClusterName;
import org.graylog2.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "Indexer/Cluster", description = "Indexer cluster information")
@Path("/system/indexer/cluster")
public class IndexerClusterResource extends RestResource {

    @Inject
    private Cluster cluster;

    @GET
    @Timed
    @Path("/name")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @ApiOperation(value = "Get the cluster name")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterName clusterName() {
        return ClusterName.create(cluster.getName());
    }

    @GET
    @Timed
    @Path("/health")
    @ApiOperation(value = "Get cluster and shard health overview")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterHealth clusterHealth() {
        final ClusterHealth.ShardStatus shards = ClusterHealth.ShardStatus.create(
                cluster.getActiveShards(),
                cluster.getInitializingShards(),
                cluster.getRelocatingShards(),
                cluster.getUnassignedShards());

        return ClusterHealth.create(cluster.getHealth().toString().toLowerCase(), shards);
    }
}
