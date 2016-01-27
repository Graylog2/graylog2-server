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
import org.graylog2.rest.resources.system.RemoteSystemProcessingResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;

@RequiresAuthentication
@Api(value = "Cluster/Processing", description = "Cluster-wide processing status control.")
@Path("/cluster/{nodeId}/processing")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterSystemProcessingResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemProcessingResource.class);

    @Inject
    public ClusterSystemProcessingResource(NodeService nodeService,
                                           RemoteInterfaceProvider remoteInterfaceProvider,
                                           @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    private RemoteSystemProcessingResource getRemoteSystemProcessingResource(String nodeId) throws NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        return remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemProcessingResource.class);
    }

    @POST
    @Timed
    @ApiOperation(value = "Pause message processing on node",
            notes = "If the message journal is enabled, incoming messages will be spooled on disk, if it is disabled, " +
                    "you might lose messages from inputs which cannot buffer themselves, like AMQP or Kafka-based inputs.")
    @Path("pause")
    public void pause(@ApiParam(name = "nodeId", value = "The id of the node where processing will be paused.", required = true)
                      @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Response response = this.getRemoteSystemProcessingResource(nodeId).pause().execute();
        if (!response.isSuccess()) {
            LOG.warn("Unable to pause message processing on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }

    @POST
    @Timed
    @ApiOperation(value = "Resume message processing on node")
    @Path("resume")
    public void resume(@ApiParam(name = "nodeId", value = "The id of the node where processing will be resumed.", required = true)
                       @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Response response = this.getRemoteSystemProcessingResource(nodeId).resume().execute();
        if (!response.isSuccess()) {
            LOG.warn("Unable to resume message processing on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }
}

