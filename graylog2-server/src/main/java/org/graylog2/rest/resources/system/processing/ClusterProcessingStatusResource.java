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
package org.graylog2.rest.resources.system.processing;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.processing.ProcessingStatusSummary;
import org.graylog2.shared.rest.resources.ProxiedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RequiresAuthentication
@Api(value = "Cluster/Processing/Status")
@Path("/cluster/processing/status")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterProcessingStatusResource extends ProxiedResource {
    @Inject
    public ClusterProcessingStatusResource(NodeService nodeService,
                                           RemoteInterfaceProvider remoteInterfaceProvider,
                                           @Context HttpHeaders httpHeaders,
                                           @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get processing status from all nodes in the cluster")
    public Map<String, Optional<ProcessingStatusSummary>> getStatus() {
        return getForAllNodes(RemoteSystemProcessingStatusResource::getStatus, createRemoteInterfaceProvider(RemoteSystemProcessingStatusResource.class));
    }

    @GET
    @Path("/persisted")
    @Timed
    @ApiOperation(value = "Get persisted processing status from all nodes in the cluster")
    public Map<String, Optional<ProcessingStatusSummary>> getPersistedStatus() {
        return getForAllNodes(RemoteSystemProcessingStatusResource::getPersistedStatus, createRemoteInterfaceProvider(RemoteSystemProcessingStatusResource.class));
    }
}
