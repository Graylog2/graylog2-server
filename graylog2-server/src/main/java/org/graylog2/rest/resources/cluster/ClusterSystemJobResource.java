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
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.resources.system.jobs.RemoteSystemJobResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiresAuthentication
@Api(value = "Cluster/Jobs", description = "Cluster-wide System Jobs")
@Path("/cluster/jobs")
public class ClusterSystemJobResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemJobResource.class);

    @Inject
    public ClusterSystemJobResource(NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider,
                                    @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @GET
    @Timed
    @ApiOperation(value = "List currently running jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<Map<String, List<SystemJobSummary>>>> list() throws IOException {
        return getForAllNodes(RemoteSystemJobResource::list, createRemoteInterfaceProvider(RemoteSystemJobResource.class));
    }

    @GET
    @Path("{jobId}")
    @Timed
    @ApiOperation(value = "Get job with the given ID")
    @Produces(MediaType.APPLICATION_JSON)
    public SystemJobSummary getJob(@ApiParam(name = "jobId", required = true) @PathParam("jobId") String jobId) throws IOException {
        for (Map.Entry<String, Node> entry : nodeService.allActive().entrySet()) {
            final RemoteSystemJobResource remoteSystemJobResource = remoteInterfaceProvider.get(entry.getValue(), this.authenticationToken, RemoteSystemJobResource.class);
            try {
                final Response<SystemJobSummary> response = remoteSystemJobResource.get(jobId).execute();
                if (response.isSuccess()) {
                    // Return early because there can be only one job with the same ID in the cluster.
                    return response.body();
                } else {
                    LOG.warn("Unable to fetch system job {} from node {}: {}", jobId, entry.getKey(), response);
                }
            } catch (IOException e) {
                LOG.warn("Unable to fetch system jobs from node {}:", entry.getKey(), e);
            }
        }

        throw new NotFoundException("System job with id " + jobId + " not found!");
    }
}
