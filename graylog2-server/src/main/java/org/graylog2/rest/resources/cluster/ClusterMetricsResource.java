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
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@RequiresAuthentication
@Api(value = "Cluster/Metrics", description = "Cluster-wide Internal Graylog metrics")
@Path("/cluster/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterMetricsResource extends ProxiedResource {

    private final Duration callTimeout;

    @Inject
    public ClusterMetricsResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                  @Named("proxied_requests_default_call_timeout") com.github.joschi.jadconfig.util.Duration defaultCallTimeout) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);

        // The metrics requests should be fast. If they are not fast, we don't want to consume
        // too many resources and timeout early
        this.callTimeout = Duration.ofMillis(Math.min(defaultCallTimeout.toMilliseconds(), 1000));
    }

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation(value = "Get all metrics of all nodes in the cluster")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    @NoAuditEvent("only used to retrieve metrics of all nodes")
    public void multipleMetricsAllNodes(@ApiParam(name = "Requested metrics", required = true)
                                        @Valid @NotNull MetricsReadRequest request,
                                        @Suspended AsyncResponse asyncResponse) {

        // Workaround to fail fast with a 401 if we can't extract an authentication token
        try {
            var ignored = getAuthenticationToken();
        } catch (NotAuthorizedException e) {
            processAsync(asyncResponse, e::getResponse);
            return;
        }

        processAsync(asyncResponse,
                () -> stripCallResult(requestOnAllNodes(RemoteMetricsResource.class, r -> r.multipleMetrics(request), callTimeout))
        );
    }
}
