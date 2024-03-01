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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import okhttp3.ResponseBody;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.graylog.plugins.views.search.rest.SearchResource.SEARCH_FORMAT_V1;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "SearchJobs", tags = {CLOUD_VISIBLE})
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
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("{nodeId}/{jobId}/status")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public void asyncSearchJobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId,
                                     @ApiParam(name = "nodeId") @PathParam("nodeId") String nodeId,
                                     @Context SearchUser searchUser,
                                     @Suspended AsyncResponse asyncResponse) {
        processAsync(asyncResponse,
                () -> {
                    final NodeResponse<ResponseBody> nodeResponse;
                    try {
                        nodeResponse = requestOnNode(nodeId, r -> r.jobStatus(jobId), RemoteSearchJobsStatusInterface.class);

                        if (nodeResponse.isSuccess()) {
                            // we cannot use try-with because the ResponseBody needs to stream the output
                            ResponseBody responseBody = nodeResponse.entity().orElseThrow();

                            try {
                                StreamingOutput streamingOutput = output -> {
                                    try {
                                        responseBody.byteStream().transferTo(output);
                                    } catch (Exception e) {
                                        responseBody.close(); // avoid leaking connections on errors
                                    }
                                };
                                return Response.ok(streamingOutput, MediaType.valueOf(MediaType.APPLICATION_JSON)).build();

                            } catch (Exception e) {
                                responseBody.close();
                            }
                        }
                        return Response.status(nodeResponse.code()).entity(nodeResponse.body()).build();

                    } catch (IOException e) {
                        return Response.serverError().entity(e.getMessage()).build();
                    }
                }
        );
    }
}
