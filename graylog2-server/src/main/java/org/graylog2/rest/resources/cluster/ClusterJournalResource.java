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
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.RemoteJournalResource;
import org.graylog2.rest.resources.system.responses.JournalSummaryResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;

@RequiresAuthentication
@Api(value = "Cluster/Journal", description = "Journal information of any nodes in the cluster")
@Path("/cluster/{nodeId}/journal")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterJournalResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterJournalResource.class);

    @Inject
    public ClusterJournalResource(NodeService nodeService,
                                  RemoteInterfaceProvider remoteInterfaceProvider,
                                  @Context HttpHeaders httpHeaders,
                                  @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get message journal information of a given node")
    @RequiresPermissions(RestPermissions.JOURNAL_READ)
    public JournalSummaryResponse get(@ApiParam(name = "nodeId", value = "The id of the node to get message journal information.", required = true)
                                      @PathParam("nodeId") String nodeId) throws IOException, NodeNotFoundException {

        var response = doNodeApiCall(nodeId, RemoteJournalResource.class, RemoteJournalResource::get, Function.identity(), null);
        if (response.isSuccess() && response.entity().isPresent()) {
            return response.entity().get();
        } else {
            LOG.warn("Unable to get message journal information on node {}: {}", nodeId, response.errorText());
            throw new WebApplicationException(response.errorText(), BAD_GATEWAY);
        }
    }
}

