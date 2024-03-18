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
import com.google.common.util.concurrent.Uninterruptibles;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
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
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Search", tags = {CLOUD_VISIBLE})
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

    @Inject
    public SearchResource(SearchDomain searchDomain,
                          SearchExecutor searchExecutor,
                          SearchJobService searchJobService,
                          EventBus serverEventBus) {
        this.searchDomain = searchDomain;
        this.searchExecutor = searchExecutor;
        this.searchJobService = searchJobService;
        this.serverEventBus = serverEventBus;
    }

    @POST
    @ApiOperation(value = "Create a search query", response = SearchDTO.class, code = 201)
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    @Consumes({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response createSearch(@ApiParam SearchDTO searchRequest, @Context SearchUser searchUser) {
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
    @ApiOperation(value = "Create a search query", response = SearchDTOv2.class, code = 201)
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    @Consumes({SEARCH_FORMAT_V2})
    @Produces({SEARCH_FORMAT_V2})
    public Response createSearchV2(@ApiParam SearchDTOv2 searchRequest, @Context SearchUser searchUser) {
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
    @ApiOperation(value = "Retrieve a search query")
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public SearchDTO getSearch(@ApiParam(name = "id") @PathParam("id") String searchId, @Context SearchUser searchUser) {
        final Search search = searchDomain.getForUser(searchId, searchUser)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));
        return SearchDTO.fromSearch(search);
    }

    @GET
    @ApiOperation(value = "Get all searches which the user may see")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public List<SearchDTO> getAllSearches(@Context SearchUser searchUser) {
        // TODO should be paginated
        final List<Search> searches = searchDomain.getAllForUser(searchUser, searchUser::canReadView);
        return searches.stream()
                .map(SearchDTO::fromSearch)
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Execute the referenced search query asynchronously",
                  notes = "Starts a new search, irrespective whether or not another is already running",
                  response = SearchJobDTO.class)
    @Path("{id}/execute")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response executeQuery(@ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam ExecutionState executionState,
                                 @Context SearchUser searchUser) {

        final SearchJob searchJob = searchExecutor.execute(id, searchUser, executionState);

        postAuditEvent(searchJob);

        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);

        return Response.created(URI.create(BASE_PATH + "/status/" + searchJobDTO.searchJobIdentifier().id()))
                .entity(searchJob)
                .build();
    }

    @POST
    @ApiOperation(value = "Explains how the referenced search would be executed", response = ExplainResults.class)
    @Path("{id}/explain")
    @NoAuditEvent("Does not return any actual data")
    public ExplainResults explainQuery(@ApiParam(name = "id") @PathParam("id") String id,
                                       @ApiParam ExecutionState executionState,
                                       @Context SearchUser searchUser) {
        return searchExecutor.explain(id, searchUser, executionState);
    }

    @POST
    @ApiOperation(value = "Execute a new synchronous search", notes = "Executes a new search and waits for its result", response = SearchJobDTO.class)
    @Path("sync")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Consumes({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public Response executeSyncJob(@ApiParam @NotNull(message = "Search body is mandatory") SearchDTO searchRequest,
                                   @ApiParam(name = "timeout", defaultValue = "60000")
                                   @QueryParam("timeout") @DefaultValue("60000") long timeout,
                                   @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();
        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());

        postAuditEvent(searchJob);

        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);

        return Response.ok(searchJobDTO).build();
    }

    @POST
    @ApiOperation(value = "Execute a new synchronous search", notes = "Executes a new search and waits for its result", response = SearchJobDTO.class)
    @Path("sync")
    @NoAuditEvent("Creating audit event manually in method body.")
    @Consumes({SEARCH_FORMAT_V2})
    @Produces({SEARCH_FORMAT_V2})
    public Response executeSyncJobv2(@ApiParam @NotNull(message = "Search body is mandatory") SearchDTOv2 searchRequest,
                                     @ApiParam(name = "timeout", defaultValue = "60000")
                                     @QueryParam("timeout") @DefaultValue("60000") long timeout,
                                     @Context SearchUser searchUser) {
        final Search search = searchRequest.toSearch();
        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());

        postAuditEvent(searchJob);

        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);

        return Response.ok(searchJobDTO).build();
    }

    @GET
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, SEARCH_FORMAT_V1})
    public SearchJobDTO jobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId, @Context SearchUser searchUser) {
        final SearchJob searchJob = searchJobService.load(jobId, searchUser.username()).orElseThrow(NotFoundException::new);
        if (searchJob != null && searchJob.getResultFuture() != null) {
            try {
                // force a "conditional join", to catch fast responses without having to poll
                Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), 5, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | TimeoutException ignore) {

            }
        }
        return SearchJobDTO.fromSearchJob(searchJob);
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
