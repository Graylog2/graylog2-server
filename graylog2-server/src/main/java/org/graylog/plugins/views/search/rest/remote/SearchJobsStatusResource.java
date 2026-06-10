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
package org.graylog.plugins.views.search.rest.remote;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import okhttp3.ResponseBody;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.ProxiedResource;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.graylog.plugins.views.search.rest.SearchResource.SEARCH_FORMAT_V1;

@PublicCloudAPI
@Tag(name = "SearchJobs")
@Path("/views/searchjobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class SearchJobsStatusResource extends ProxiedResource implements PluginRestResource {

    @Inject
    public SearchJobsStatusResource(@Context HttpHeaders httpHeaders,
                                    NodeService nodeService,
                                    RemoteInterfaceProvider remoteInterfaceProvider,
                                    ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Operation(summary = "Retrieve the status of an executed query")
    @Path("{nodeId}/{jobId}/status")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public void asyncSearchJobStatus(@Parameter(name = "jobId", required = true) @NotBlank @PathParam("jobId") String jobId,
                                     @Parameter(name = "nodeId", required = true) @NotBlank @PathParam("nodeId") String nodeId,
                                     @Parameter(name = "page") @QueryParam("page") @DefaultValue("0") int page,
                                     @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("0") int perPage,
                                     @Context SearchUser searchUser,
                                     @Suspended AsyncResponse asyncResponse) {
        processAsync(asyncResponse,
                () -> {
                    try {
                        final NodeResponse<ResponseBody> nodeResponse = requestOnNode(nodeId, r -> r.jobStatus(jobId, page, perPage), RemoteSearchJobsStatusInterface.class);
                        return RestTools.streamResponse(nodeResponse, MediaType.APPLICATION_JSON, null);
                    } catch (IOException e) {
                        return Response.serverError().entity(e.getMessage()).build();
                    }
                }
        );
    }

    @DELETE
    @Operation(summary = "Cancels search job")
    @Path("{nodeId}/{jobId}/cancel")
    @Produces({MediaType.APPLICATION_JSON})
    @NoAuditEvent("this is a proxy resource, the event will be triggered on the individual nodes")
    public void cancelAsyncSearchJob(@Parameter(name = "jobId", required = true) @NotBlank @PathParam("jobId") String jobId,
                                     @Parameter(name = "nodeId", required = true) @NotBlank @PathParam("nodeId") String nodeId,
                                     @Context SearchUser searchUser,
                                     @Suspended AsyncResponse asyncResponse) {
        processAsync(asyncResponse,
                () -> {
                    try {
                        final NodeResponse<Void> nodeResponse = requestOnNode(nodeId, r -> r.cancelJob(jobId), RemoteSearchJobsStatusInterface.class);
                        return Response.status(Response.Status.fromStatusCode(nodeResponse.code())).build();
                    } catch (IOException e) {
                        return Response.serverError().entity(e.getMessage()).build();
                    }
                }
        );
    }
}
