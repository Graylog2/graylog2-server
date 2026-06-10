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
package org.graylog.plugins.views.search.rest;

import com.google.common.eventbus.EventBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.processor.systemnotification.TemplateRenderRequest;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.ExplainResults;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@PublicCloudAPI
@Tag(name = "Search")
@Path("/views/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class SearchResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);
    private static final String BASE_PATH = "views/search";
    public static final String SEARCH_FORMAT_V1 = "application/vnd.graylog.search.v1+json";
    public static final String SEARCH_FORMAT_V2 = "application/vnd.graylog.search.v2+json";

    private final SearchDomain searchDomain;
    private final SearchExecutor searchExecutor;
    private final SearchJobService searchJobService;
    private final EventBus serverEventBus;

    private final ClusterConfigService clusterConfigService;

    @Inject
    public SearchResource(final SearchDomain searchDomain,
                          final SearchExecutor searchExecutor,
                          final SearchJobService searchJobService,
                          final EventBus serverEventBus,
                          final ClusterConfigService clusterConfigService) {
        this.searchDomain = searchDomain;
        this.searchExecutor = searchExecutor;
        this.searchJobService = searchJobService;
        this.serverEventBus = serverEventBus;
        this.clusterConfigService = clusterConfigService;
    }

    @POST
    @Operation(summary = "Create a search query")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Create a search query successful",
                    content = @Content(schema = @Schema(implementation = SearchDTO.class)))
    })
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    @Consumes({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response createSearch(@RequestBody(required = true) SearchDTO searchRequest, @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();

        final Search saved = searchDomain.saveForUser(search, searchUser);
        final SearchDTO result = SearchDTO.fromSearch(saved);
        if (result == null || result.id() == null) {
            return Response.serverError().build();
        }
        LOG.debug("Created new search object {}", result.id());
        return Response.created(URI.create(result.id())).entity(result).build();
    }

    @POST
    @Operation(summary = "Create a search query")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Create a search query successful",
                         content = @Content(schema = @Schema(implementation = SearchDTOv2.class)))
    })
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    @Consumes({SEARCH_FORMAT_V2})
    @Produces({SEARCH_FORMAT_V2})
    public Response createSearchV2(SearchDTOv2 searchRequest, @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();

        final Search saved = searchDomain.saveForUser(search, searchUser);
        final SearchDTOv2 result = SearchDTOv2.fromSearch(saved);
        if (result == null || result.id() == null) {
            return Response.serverError().build();
        }
        LOG.debug("Created new search object {}", result.id());
        return Response.created(URI.create(result.id())).entity(result).build();
    }

    @GET
    @Operation(summary = "Retrieve a search query")
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public SearchDTO getSearch(@Parameter(name = "id") @PathParam("id") String searchId, @Context SearchUser searchUser) {
        final Search search = searchDomain.getForUser(searchId, searchUser)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));
        return SearchDTO.fromSearch(search);
    }

    @GET
    @Operation(summary = "Get all searches which the user may see")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public List<SearchDTO> getAllSearches(@Context SearchUser searchUser) {
        // TODO should be paginated
        final List<Search> searches = searchDomain.getAllForUser(searchUser, searchUser::canReadView);
        return searches.stream()
                .map(SearchDTO::fromSearch)
                .collect(Collectors.toList());
    }

    @POST
    @Operation(summary = "Execute the referenced search query asynchronously",
                  description = "Starts a new search, irrespective whether or not another is already running")
    @Path("{id}/execute")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response executeQuery(@Parameter(name = "id") @PathParam("id") String id,
                                 @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ExecutionState.class)))
                                 ExecutionState executionState,
                                 @Context SearchUser searchUser) {

        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        final ExecutionState enrichedExecutionState = executionState == null ?
                ExecutionState.empty().withDefaultQueryCancellationIfNotSpecified(searchesClusterConfig) : executionState.withDefaultQueryCancellationIfNotSpecified(searchesClusterConfig);

        final SearchJob searchJob = searchExecutor.executeAsync(id, searchUser, enrichedExecutionState);

        postAuditEvent(searchJob);

        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);

        return Response.created(URI.create(BASE_PATH + "/status/" + searchJobDTO.searchJobIdentifier().id()))
                .entity(searchJob)
                .build();
    }

    @POST
    @Operation(summary = "Explains how the referenced search would be executed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Explains how the referenced search would be executed retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExplainResults.class)))
    })
    @Path("{id}/explain")
    @NoAuditEvent("Does not return any actual data")
    public ExplainResults explainQuery(@Parameter(name = "id") @PathParam("id") String id,
                                       @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ExecutionState.class)))
                                       ExecutionState executionState,
                                       @Context SearchUser searchUser) {
        return searchExecutor.explain(id, searchUser, executionState);
    }

    @POST
    @Operation(summary = "Execute a new synchronous search", description = "Executes a new search and waits for its result")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execute a new synchronous search retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SearchJobDTO.class)))
    })
    @Path("sync")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Consumes({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response executeSyncJob(@RequestBody(required = true) @NotNull(message = "Search body is mandatory") SearchDTO searchRequest,
                                   @Parameter(name = "timeout")
                                   @QueryParam("timeout") @DefaultValue("60000") @Deprecated long timeout,
                                   @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();
        return executeSyncJobInner(search, searchUser);
    }

    @POST
    @Operation(summary = "Execute a new synchronous search", description = "Executes a new search and waits for its result")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execute a new synchronous search retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SearchJobDTO.class)))
    })
    @Path("sync")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Consumes({SEARCH_FORMAT_V2})
    @Produces({SEARCH_FORMAT_V2})
    public Response executeSyncJobv2(@RequestBody(required = true) @NotNull(message = "Search body is mandatory") SearchDTOv2 searchRequest,
                                     @Parameter(name = "timeout")
                                     @QueryParam("timeout") @DefaultValue("60000") @Deprecated long timeout,
                                     @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();
        return executeSyncJobInner(search, searchUser);
    }

    private Response executeSyncJobInner(final Search search, final SearchUser searchUser) {
        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        final ExecutionState enrichedExecutionState = ExecutionState.empty().withDefaultQueryCancellationIfNotSpecified(searchesClusterConfig);
        final SearchJob searchJob = searchExecutor.executeSync(search, searchUser, enrichedExecutionState);

        postAuditEvent(searchJob);

        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);

        return Response.ok(searchJobDTO).build();
    }

    @GET
    @Operation(summary = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response jobStatus(@Parameter(name = "jobId") @PathParam("jobId") String jobId,
                              @Parameter(name = "page") @QueryParam("page") @DefaultValue("0") int page,
                              @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("0") int perPage,
                              @Context SearchUser searchUser) {
        return Response
                .ok(
                        searchJobService.load(jobId, searchUser)
                                .map(searchJobDTO -> searchJobDTO.withResultsLimitedTo(page, perPage))
                                .orElseThrow(NotFoundException::new)
                )
                .build();
    }

    @DELETE
    @Path("cancel/{jobId}")
    @NoAuditEvent("To be decided if we want to have cancellation of jobs in audit log")
    @Produces({MediaType.APPLICATION_JSON})
    public Response cancelJob(@PathParam("jobId") String jobId,
                              @Context SearchUser searchUser) {

        if (!searchJobService.cancel(jobId, searchUser)) {
            throw new NotFoundException();
        }
        return Response.ok().build();
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }

}
