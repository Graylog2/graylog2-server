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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.stats.ClusterStats;
import org.graylog2.system.stats.ClusterStatsService;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.mongo.MongoStats;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "System/ClusterStats", description = "Cluster stats")
@Path("/system/cluster/stats")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterStatsResource extends RestResource {
    private final ClusterStatsService clusterStatsService;

    @Inject
    public ClusterStatsResource(ClusterStatsService clusterStatsService) {
        this.clusterStatsService = clusterStatsService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Cluster status information.",
            notes = "This resource returns information about the Graylog cluster.")
    public ClusterStats systemStats() {
        return clusterStatsService.clusterStats();
    }

    @GET
    @Path("/elasticsearch")
    @Timed
    @ApiOperation(value = "Elasticsearch information.",
            notes = "This resource returns information about the Elasticsearch Cluster.")
    public ElasticsearchStats elasticsearchStats() {
        return clusterStatsService.elasticsearchStats();
    }

    @GET
    @Path("/mongo")
    @Timed
    @ApiOperation(value = "MongoDB information.",
            notes = "This resource returns information about MongoDB.")
    public MongoStats mongoStats() {
        return clusterStatsService.mongoStats();
    }

}
