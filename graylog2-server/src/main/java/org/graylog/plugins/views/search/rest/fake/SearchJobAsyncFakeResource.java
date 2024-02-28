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
package org.graylog.plugins.views.search.rest.fake;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.Api;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Search", tags = {CLOUD_VISIBLE})
@Path("/fake/searchjob")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class SearchJobAsyncFakeResource extends ProxiedResource implements PluginRestResource {

    private static Map<SearchJobId, SearchJobStatus> fakeSearchJobs = new HashMap<>();
    private static int latestNodeJobCounter = 0;

    private final SearchExecutor searchExecutor;
    private final String currentNodeId;

    @Inject
    public SearchJobAsyncFakeResource(HttpHeaders httpHeaders,
                                      NodeService nodeService,
                                      RemoteInterfaceProvider remoteInterfaceProvider,
                                      ExecutorService executorService,
                                      NodeId nodeId,
                                      SearchExecutor searchExecutor) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.searchExecutor = searchExecutor;
        this.currentNodeId = nodeId.getNodeId();
    }

    @POST
    @Path("{gl_search_id}/start")
    @NoAuditEvent("Fake endpoint")
    @Produces({MediaType.APPLICATION_JSON})
    public Response start(@PathParam("gl_search_id") String graylogSearchId,
                          @Context SearchUser searchUser) {

        SearchJobId searchJobId = new SearchJobId(searchUser.getUser().getId(), currentNodeId, String.valueOf(latestNodeJobCounter++));
        fakeSearchJobs.put(searchJobId, new SearchJobStatus(graylogSearchId));

        return Response.accepted()
                .entity(searchJobId)
                .build();
    }

    @GET
    @Path("{node_id}/{asyncSearchId}/status")
    @NoAuditEvent("Fake endpoint")
    @Produces({MediaType.APPLICATION_JSON})
    public Response poll(@PathParam("asyncSearchId") String asyncSearchId,
                         @PathParam("node_id") String nodeId,
                         @Context SearchUser searchUser) {

        SearchJobId searchJobId = new SearchJobId(searchUser.getUser().getId(), nodeId, asyncSearchId);
        final SearchJobStatus searchJobStatus = fakeSearchJobs.get(searchJobId);
        if (searchJobStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            searchJobStatus.pollCount++;
            if (searchJobStatus.isCancelled) {
                fakeSearchJobs.remove(searchJobId);
                return Response.status(Response.Status.GONE)
                        .entity(searchJobId)
                        .build();
            } else if (searchJobStatus.pollCount > 5) {
                if (searchJobStatus.pollCount > 8) {
                    fakeSearchJobs.remove(searchJobId);
                }
                return Response.ok()
                        .entity(executeSyncSearchInSimulation(searchJobStatus.glSearchToExecuteInSimulation, searchUser))
                        .build();
            } else {
                return Response.accepted()
                        .build();
            }
        }
    }

    @DELETE
    @Path("{node_id}/{asyncSearchId}/cancel")
    @NoAuditEvent("Fake endpoint")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete(@PathParam("asyncSearchId") String asyncSearchId,
                           @PathParam("node_id") String nodeId,
                           @Context SearchUser searchUser) {
        SearchJobId searchJobId = new SearchJobId(searchUser.getUser().getId(), nodeId, asyncSearchId);
        final SearchJobStatus searchJobStatus = fakeSearchJobs.get(searchJobId);
        if (searchJobStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            searchJobStatus.isCancelled = true;
            return Response.ok().build();
        }

    }


    private SearchJobDTO executeSyncSearchInSimulation(final String glSearchId,
                                                       final SearchUser searchUser) {
        final SearchJob searchJob = searchExecutor.execute(glSearchId, searchUser, ExecutionState.empty());
        return SearchJobDTO.fromSearchJob(searchJob);
    }


    public record SearchJobId(@JsonIgnore String userID, String nodeID, String asyncSearchId) {}

    class SearchJobStatus {
        int pollCount = 0;
        boolean isCancelled = false;
        String glSearchToExecuteInSimulation;

        public SearchJobStatus(String glSearchToExecuteInSimulation) {
            this.glSearchToExecuteInSimulation = glSearchToExecuteInSimulation;
        }
    }
}
