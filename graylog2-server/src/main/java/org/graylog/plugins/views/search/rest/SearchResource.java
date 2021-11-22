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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Api(value = "Search")
@Path("/views/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON})
@RequiresAuthentication
public class SearchResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);
    private static final String BASE_PATH = "views/search";

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
    @ApiOperation(value = "Create a search query", response = Search.class, code = 201)
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    public Response createSearch(@ApiParam Search search, @Context SearchUser searchUser) {
        final Search saved = searchDomain.saveForUser(search, searchUser);
        if (saved == null || saved.id() == null) {
            return Response.serverError().build();
        }
        LOG.debug("Created new search object {}", saved.id());
        return Response.created(URI.create(saved.id())).entity(saved).build();
    }

    @GET
    @ApiOperation(value = "Retrieve a search query")
    @Path("{id}")
    public Search getSearch(@ApiParam(name = "id") @PathParam("id") String searchId, @Context SearchUser searchUser) {
        return searchDomain.getForUser(searchId, searchUser)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));
    }

    @GET
    @ApiOperation(value = "Get all searches which the user may see")
    public List<Search> getAllSearches(@Context SearchUser searchUser) {
        // TODO should be paginated
        return searchDomain.getAllForUser(searchUser, searchUser::canReadView);
    }

    @POST
    @ApiOperation(value = "Execute the referenced search query asynchronously",
                  notes = "Starts a new search, irrespective whether or not another is already running",
                  response = SearchJob.class)
    @Path("{id}/execute")
    @NoAuditEvent("Creating audit event manually in method body.")
    public Response executeQuery(@ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam ExecutionState executionState,
                                 @Context SearchUser searchUser) {
        final SearchJob searchJob = searchExecutor.execute(id, searchUser, executionState);

        postAuditEvent(searchJob);

        return Response.created(URI.create(BASE_PATH + "/status/" + searchJob.getId()))
                .entity(searchJob)
                .build();
    }

    @POST
    @ApiOperation(value = "Execute a new synchronous search", notes = "Executes a new search and waits for its result", response = SearchJob.class)
    @Path("sync")
    @NoAuditEvent("Creating audit event manually in method body.")
    public Response executeSyncJob(@ApiParam @NotNull(message = "Search body is mandatory") Search search,
                                   @ApiParam(name = "timeout", defaultValue = "60000")
                                   @QueryParam("timeout") @DefaultValue("60000") long timeout,
                                   @Context SearchUser searchUser) {
        final SearchJob searchJob = searchExecutor.execute(search, searchUser, ExecutionState.empty());

        postAuditEvent(searchJob);

        return Response.ok(searchJob).build();
    }

    @GET
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    public SearchJob jobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId, @Context SearchUser searchUser) {
        final SearchJob searchJob = searchJobService.load(jobId, searchUser.username()).orElseThrow(NotFoundException::new);
        try {
            // force a "conditional join", to catch fast responses without having to poll
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), 5, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException ignore) {
        }
        return searchJob;
    }

    private void postAuditEvent(SearchJob searchJob) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(getCurrentUser(), searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
