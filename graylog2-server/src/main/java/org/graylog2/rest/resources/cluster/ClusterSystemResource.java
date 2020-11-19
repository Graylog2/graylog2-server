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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemProcessBufferDumpResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.inject.Named;
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
import java.util.concurrent.ExecutorService;

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
                                 @Context HttpHeaders httpHeaders,
                                 @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
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
    public SystemJVMResponse jvm(@ApiParam(name = "nodeId", value = "The id of the node to retrieve JVM information.", required = true)
                                 @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemResource.class);
        final Response<SystemJVMResponse> response = remoteSystemResource.jvm().execute();
        if (response.isSuccessful()) {
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
    public SystemThreadDumpResponse threadDump(@ApiParam(name = "nodeId", value = "The id of the node to get a thread dump.", required = true)
                                               @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemResource.class);
        final Response<SystemThreadDumpResponse> response = remoteSystemResource.threadDump().execute();
        if (response.isSuccessful()) {
            return response.body();
        } else {
            LOG.warn("Unable to get thread dump on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a process buffer dump of the given node")
    @RequiresPermissions(RestPermissions.PROCESSBUFFER_DUMP)
    @Path("{nodeId}/processbufferdump")
    public SystemProcessBufferDumpResponse processBufferDump(@ApiParam(name = "nodeId", value = "The id of the node to get a process buffer dump.", required = true)
                                               @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {
        final Node targetNode = nodeService.byNodeId(nodeId);

        final RemoteSystemResource remoteSystemResource = remoteInterfaceProvider.get(targetNode,
                this.authenticationToken,
                RemoteSystemResource.class);
        final Response<SystemProcessBufferDumpResponse> response = remoteSystemResource.processBufferDump().execute();
        if (response.isSuccessful()) {
            return response.body();
        } else {
            LOG.warn("Unable to get process buffer dump on node {}: {}", nodeId, response.message());
            throw new WebApplicationException(response.message(), BAD_GATEWAY);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a process buffer dump of all cluster nodes")
    @RequiresPermissions(RestPermissions.PROCESSBUFFER_DUMP)
    @Path("processbufferdump")
    public Map<String, Optional<SystemProcessBufferDumpResponse>> clusterProcessBufferDump() {
        return getForAllNodes(RemoteSystemResource::processBufferDump, createRemoteInterfaceProvider(RemoteSystemResource.class));
    }
}

