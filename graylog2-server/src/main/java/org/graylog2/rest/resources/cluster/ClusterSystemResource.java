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
import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;

@RequiresAuthentication
@Api(value = "Cluster", description = "System information of all nodes in the cluster")
@Path("/cluster")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterSystemResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemResource.class);

    @Inject
    public ClusterSystemResource(NodeService nodeService,
                                 RemoteInterfaceProvider remoteInterfaceProvider,
                                 @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get system overview of all Graylog nodes")
    public Map<String, Optional<SystemOverviewResponse>> get() {
        return getForAllNodes(RemoteSystemResource::system, createRemoteInterfaceProvider(RemoteSystemResource.class));
    }

    @GET
    @Timed
    @ApiOperation(value = "Get JVM information of the given node")
    @Path("{nodeId}/jvm")
    public SystemJVMResponse jvm(@ApiParam(name = "nodeId", value = "The id of the node where processing will be paused.", required = true)
                                 @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemResource.class);
        final Response<SystemJVMResponse> response = remoteSystemResource.jvm().execute();
        if (response.isSuccess()) {
            return response.body();
        } else {
            LOG.warn("Unable to get jvm information on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a thread dump of the given node")
    @RequiresPermissions(RestPermissions.THREADS_DUMP)
    @Path("{nodeId}/threaddump")
    public SystemThreadDumpResponse threadDump(@ApiParam(name = "nodeId", value = "The id of the node where processing will be paused.", required = true)
                                               @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemResource.class);
        final Response<SystemThreadDumpResponse> response = remoteSystemResource.threadDump().execute();
        if (response.isSuccess()) {
            return response.body();
        } else {
            LOG.warn("Unable to get thread dump on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }
}

