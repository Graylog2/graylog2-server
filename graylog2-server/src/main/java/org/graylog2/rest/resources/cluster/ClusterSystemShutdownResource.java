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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.RemoteSystemShutdownResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;

@RequiresAuthentication
@Api(value = "Cluster/Shutdown", description = "Shutdown gracefully nodes in cluster")
@Path("/cluster/{nodeId}/shutdown")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterSystemShutdownResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemShutdownResource.class);

    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final String authenticationToken;

    @Inject
    public ClusterSystemShutdownResource(NodeService nodeService,
                                         RemoteInterfaceProvider remoteInterfaceProvider,
                                         @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;

        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.authenticationToken = authenticationTokens.get(0);
        } else {
            this.authenticationToken = null;
        }
    }

    @POST
    @Timed
    @ApiOperation(value = "Shutdown node gracefully.",
            notes = "Attempts to process all buffered and cached messages before exiting, " +
                    "shuts down inputs first to make sure that no new messages are accepted.")
    public void shutdown(@ApiParam(name = "nodeId", value = "The id of the node to shutdown.", required = true)
                         @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        RemoteSystemShutdownResource remoteSystemShutdownResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemShutdownResource.class);
        final Response response = remoteSystemShutdownResource.shutdown().execute();
        if (response.code() != ACCEPTED.getCode()) {
            LOG.warn("Unable send shut down signal to node " + nodeId + ": " + response.message());
        }
    }
}

