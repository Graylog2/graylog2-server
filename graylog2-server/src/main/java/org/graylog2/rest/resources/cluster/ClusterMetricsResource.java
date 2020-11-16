/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.cluster;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RequiresAuthentication
@Api(value = "Cluster/Metrics", description = "Cluster-wide Internal Graylog metrics")
@Path("/cluster/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterMetricsResource extends ProxiedResource {

    @Inject
    public ClusterMetricsResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation(value = "Get all metrics of all nodes in the cluster")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    @NoAuditEvent("only used to retrieve metrics of all nodes")
    public Map<String, Optional<MetricsSummaryResponse>> multipleMetricsAllNodes(@ApiParam(name = "Requested metrics", required = true)
                                                                                 @Valid @NotNull MetricsReadRequest request) throws IOException, NodeNotFoundException {
        return getForAllNodes(remoteMetricsResource -> remoteMetricsResource.multipleMetrics(request), createRemoteInterfaceProvider(RemoteMetricsResource.class));
    }
}
