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
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.resources.system.jobs.RemoteSystemJobResource;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RequiresAuthentication
@Api(value = "Cluster/Jobs", description = "Cluster-wide System Jobs")
@Path("/cluster/jobs")
public class ClusterSystemJobResource extends ProxiedResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterSystemJobResource.class);

    @Inject
    public ClusterSystemJobResource(NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider,
                                    @Context HttpHeaders httpHeaders,
                                    @Named("proxiedRequestsExecutorService") ExecutorService executorService) throws NodeNotFoundException {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Timed
    @ApiOperation(value = "List currently running jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Optional<Map<String, List<SystemJobSummary>>>> list() throws IOException {
        return getForAllNodes(RemoteSystemJobResource::list, createRemoteInterfaceProvider(RemoteSystemJobResource.class));
    }

    @GET
    @Path("{jobId}")
    @Timed
    @ApiOperation(value = "Get job with the given ID")
    @Produces(MediaType.APPLICATION_JSON)
    public SystemJobSummary getJob(@ApiParam(name = "jobId", required = true) @PathParam("jobId") String jobId) throws IOException {
        for (Map.Entry<String, Node> entry : nodeService.allActive().entrySet()) {
            final RemoteSystemJobResource remoteSystemJobResource = remoteInterfaceProvider.get(entry.getValue(), this.authenticationToken, RemoteSystemJobResource.class);
            try {
                final Response<SystemJobSummary> response = remoteSystemJobResource.get(jobId).execute();
                if (response.isSuccessful()) {
                    // Return early because there can be only one job with the same ID in the cluster.
                    return response.body();
                }
            } catch (IOException e) {
                LOG.warn("Unable to fetch system jobs from node {}:", entry.getKey(), e);
            }
        }

        throw new NotFoundException("System job with id " + jobId + " not found!");
    }

    @DELETE
    @Path("{jobId}")
    @Timed
    @ApiOperation(value = "Cancel job with the given ID")
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_STOP)
    public SystemJobSummary cancelJob(@ApiParam(name = "jobId", required = true) @PathParam("jobId") @NotEmpty String jobId) throws IOException {
        final Optional<Response<SystemJobSummary>> summaryResponse = nodeService.allActive().entrySet().stream()
                .map(entry -> {
                    final RemoteSystemJobResource resource = remoteInterfaceProvider.get(entry.getValue(),
                            this.authenticationToken, RemoteSystemJobResource.class);
                    try {
                        return resource.delete(jobId).execute();
                    } catch (IOException e) {
                        LOG.warn("Unable to fetch system jobs from node {}:", entry.getKey(), e);
                        return null;
                    }
                })
                .filter(response -> response != null && response.isSuccessful())
                .findFirst(); // There should be only one job with the given ID in the cluster. Just take the first one.

        return summaryResponse
                .orElseThrow(() -> new NotFoundException("System job with ID <" + jobId + "> not found!"))
                .body();
    }
}
