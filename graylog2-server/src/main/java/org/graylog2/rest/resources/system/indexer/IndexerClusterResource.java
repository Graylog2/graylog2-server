/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.rest.models.system.indexer.responses.ClusterName;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Locale;

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
        final JsonNode health = cluster.health()
                .orElseThrow(() -> new InternalServerErrorException("Couldn't read Elasticsearch cluster health"));
        final String clusterName = health.path("cluster_name").asText("<unknown>");
        return ClusterName.create(clusterName);
    }

    @GET
    @Timed
    @Path("/health")
    @ApiOperation(value = "Get cluster and shard health overview")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterHealth clusterHealth() {
        final JsonNode health = cluster.health()
                .orElseThrow(() -> new InternalServerErrorException("Couldn't read Elasticsearch cluster health"));
        final ClusterHealth.ShardStatus shards = ClusterHealth.ShardStatus.create(
                health.path("active_shards").asInt(),
                health.path("initializing_shards").asInt(),
                health.path("relocating_shards").asInt(),
                health.path("unassigned_shards").asInt());

        return ClusterHealth.create(health.path("status").asText().toLowerCase(Locale.ENGLISH), shards);
    }
}
