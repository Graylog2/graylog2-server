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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;
import org.graylog2.shared.security.RestPermissions;
import retrofit2.Response;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
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
@Api(value = "Cluster/Metrics", description = "Cluster-wide Internal Graylog metrics")
@Path("/cluster/{nodeId}/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterMetricsResource extends ProxiedResource {
    @Inject
    public ClusterMetricsResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders) {
        super(httpHeaders, nodeService, remoteInterfaceProvider);
    }

    private RemoteMetricsResource getResourceForNode(String nodeId) throws NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);
        return remoteInterfaceProvider.get(targetNode, this.authenticationToken, RemoteMetricsResource.class);
    }

    @GET
    @Timed
    @Path("/names")
    @ApiOperation(value = "Get all metrics keys/names from node")
    @RequiresPermissions(RestPermissions.METRICS_ALLKEYS)
    public MetricNamesResponse metricNames(@ApiParam(name = "nodeId", value = "The id of the node whose metrics we want.", required = true)
                                           @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Response<MetricNamesResponse> result = getResourceForNode(nodeId).metricNames().execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), BAD_GATEWAY);
        }
    }

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation("Get the values of multiple metrics at once from node")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    public MetricsSummaryResponse multipleMetrics(@ApiParam(name = "nodeId", value = "The id of the node whose metrics we want.", required = true)
                                                  @PathParam("nodeId") String nodeId,
                                                  @ApiParam(name = "Requested metrics", required = true)
                                                  @Valid @NotNull MetricsReadRequest request) throws IOException, NodeNotFoundException {
        final Response<MetricsSummaryResponse> result = getResourceForNode(nodeId).multipleMetrics(request).execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), BAD_GATEWAY);
        }
    }

    @GET
    @Timed
    @Path("/namespace/{namespace}")
    @ApiOperation(value = "Get all metrics of a namespace from node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric namespace")
    })
    public MetricsSummaryResponse byNamespace(@ApiParam(name = "nodeId", value = "The id of the node whose metrics we want.", required = true)
                                              @PathParam("nodeId") String nodeId,
                                              @ApiParam(name = "namespace", required = true)
                                              @PathParam("namespace") String namespace) throws IOException, NodeNotFoundException {
        final Response<MetricsSummaryResponse> result = getResourceForNode(nodeId).byNamespace(namespace).execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), BAD_GATEWAY);
        }
    }
}
