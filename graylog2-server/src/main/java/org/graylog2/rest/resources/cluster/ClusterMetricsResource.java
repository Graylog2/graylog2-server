package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;
import org.graylog2.shared.security.RestPermissions;
import retrofit.Response;

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
import java.util.List;

@RequiresAuthentication
@Api(value = "Cluster/Metrics", description = "Cluster-wide Internal Graylog2 metrics")
@Path("/cluster/{nodeId}/metrics")
public class ClusterMetricsResource {
    private final RemoteMetricsResource remoteMetricsResource;

    @Inject
    public ClusterMetricsResource(@PathParam("nodeId") String nodeId,
                                  NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders) throws NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final List<String> authenticationTokens = httpHeaders.getRequestHeader("Authorization");
        if (authenticationTokens != null && authenticationTokens.size() >= 1) {
            this.remoteMetricsResource = remoteInterfaceProvider.get(targetNode, authenticationTokens.get(0), RemoteMetricsResource.class);
        } else {
            throw new UnauthorizedException();
        }
    }

    @GET
    @Timed
    @Path("/names")
    @ApiOperation(value = "Get all metrics keys/names from node")
    @RequiresPermissions(RestPermissions.METRICS_ALLKEYS)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricNamesResponse metricNames() throws IOException {
        final Response<MetricNamesResponse> result =  remoteMetricsResource.metricNames().execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), result.code());
        }
    }

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation("Get the values of multiple metrics at once from node")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    public MetricsSummaryResponse multipleMetrics(@ApiParam(name = "Requested metrics", required = true)
                                                  @Valid @NotNull MetricsReadRequest request) throws IOException {
        final Response<MetricsSummaryResponse> result = remoteMetricsResource.multipleMetrics(request).execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), result.code());
        }
    }

    @GET
    @Timed
    @Path("/namespace/{namespace}")
    @ApiOperation(value = "Get all metrics of a namespace from node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric namespace")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsSummaryResponse byNamespace(@ApiParam(name = "namespace", required = true)
                                              @PathParam("namespace") String namespace) throws IOException {
        final Response<MetricsSummaryResponse> result = remoteMetricsResource.byNamespace(namespace).execute();
        if (result.isSuccess()) {
            return result.body();
        } else {
            throw new WebApplicationException(result.message(), result.code());
        }
    }
}
