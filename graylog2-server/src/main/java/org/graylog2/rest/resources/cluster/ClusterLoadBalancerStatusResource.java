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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteLoadBalancerStatusResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.PUT;
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
@Api(value = "Cluster/LoadBalancers", description = "Cluster-wide status propagation for LB")
@Produces(MediaType.APPLICATION_JSON)
@Path("/cluster/{nodeId}/lbstatus")
public class ClusterLoadBalancerStatusResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterLoadBalancerStatusResource.class);

    @Inject
    public ClusterLoadBalancerStatusResource(NodeService nodeService,
                                             RemoteInterfaceProvider remoteInterfaceProvider,
                                             @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @PUT
    @Timed
    @RequiresAuthentication
    @RequiresPermissions(RestPermissions.LBSTATUS_CHANGE)
    @ApiOperation(value = "Override load balancer status of this graylog2-server node. Next lifecycle " +
            "change will override it again to its default. Set to ALIVE or DEAD.")
    @Path("/override/{status}")
    public void override(@ApiParam(name = "nodeId", value = "The id of the node whose LB status will be changed", required = true)
                         @PathParam("nodeId") String nodeId,
                         @ApiParam(name = "status") @PathParam("status") String status) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        RemoteLoadBalancerStatusResource remoteLoadBalancerStatusResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteLoadBalancerStatusResource.class);
        final Response response = remoteLoadBalancerStatusResource.override(status).execute();
        if (!response.isSuccess()) {
            LOG.warn("Unable to override load balancer status on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }
}

