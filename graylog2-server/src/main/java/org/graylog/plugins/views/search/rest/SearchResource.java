/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import one.util.streamex.StreamEx;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchMetadata;
import org.graylog.plugins.views.search.ViewsUser;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Api(value = "Search")
@Path("/views/search")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SearchResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    private static final String BASE_PATH = "views/search";

    private final QueryEngine queryEngine;
    private final SearchJobService searchJobService;
    private final SearchDomain searchDomain;

    @Inject
    public SearchResource(QueryEngine queryEngine,
                          SearchJobService searchJobService,
                          SearchDomain searchDomain) {
        this.queryEngine = queryEngine;
        this.searchJobService = searchJobService;
        this.searchDomain = searchDomain;
    }

    @POST
    @ApiOperation(value = "Create a search query", response = Search.class, code = 201)
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_CREATE)
    public Response createSearch(@ApiParam Search search) {
        final Search saved = searchDomain.create(search, viewsUser());

        LOG.debug("Created new search object {}", saved.id());

        return Response.created(URI.create(Objects.requireNonNull(saved.id()))).entity(saved).build();
    }

    private ViewsUser viewsUser() {
        return ViewsUser.fromDbUser(
                getCurrentUser(),
                streamId -> isPermitted(RestPermissions.STREAMS_READ, streamId),
                viewId -> isPermitted(ViewsRestPermissions.VIEW_READ, viewId),
                this::isPermitted);
    }

    private String username() {
        return getCurrentUser() != null ? getCurrentUser().getName() : null;
    }

    @GET
    @ApiOperation(value = "Retrieve a search query")
    @Path("{id}")
    public Search getSearch(@ApiParam(name = "id") @PathParam("id") String searchId) {
        return searchDomain.find(searchId, viewsUser());
    }

    @GET
    @ApiOperation(value = "Get all searches which the user may see")
    public List<Search> getAllSearches() {
        // TODO should be paginated
        return searchDomain.getAllForUser(viewsUser());
    }

    @POST
    @ApiOperation(value = "Execute the referenced search query asynchronously",
            notes = "Starts a new search, irrespective whether or not another is already running")
    @Path("{id}/execute")
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_JOB_CREATE)
    public Response executeQuery(@ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam Map<String, Object> executionState) {

        final SearchJob runningSearchJob = searchDomain.executeAsync(id, executionState, viewsUser());

        return Response.created(URI.create(BASE_PATH + "/status/" + runningSearchJob.getId()))
                .entity(runningSearchJob)
                .build();
    }

    @POST
    @ApiOperation(value = "Execute a new synchronous search", notes = "Executes a new search and waits for its result")
    @Path("sync")
    @AuditEvent(type = ViewsAuditEventTypes.SEARCH_EXECUTE)
    public Response executeSyncJob(@ApiParam Search search,
                                   @ApiParam(name = "timeout", defaultValue = "60000")
                                   @QueryParam("timeout") @DefaultValue("60000") long timeout) {
        final SearchJob finishedSearchJob = searchDomain.executeSync(search, viewsUser(), timeout);

        return Response.ok(finishedSearchJob).build();
    }

    @GET
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    public SearchJob jobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId) {
        final SearchJob searchJob = searchJobService.load(jobId, username()).orElseThrow(NotFoundException::new);
        try {
            // force a "conditional join", to catch fast responses without having to poll
            //noinspection UnstableApiUsage
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), 5, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException ignore) {
        }
        return searchJob;
    }

    @GET
    @ApiOperation(value = "Metadata for the given Search object", notes = "Used for already persisted search objects")
    @Path("metadata/{searchId}")
    public SearchMetadata metadata(@ApiParam("searchId") @PathParam("searchId") String searchId) {
        final Search search = getSearch(searchId);
        return metadataForObject(search);
    }

    @POST
    @ApiOperation(value = "Metadata for the posted Search object", notes = "Intended for search objects that aren't yet persisted (e.g. for validation or interactive purposes)")
    @Path("metadata")
    @NoAuditEvent("Only returning metadata for given search, not changing any data")
    public SearchMetadata metadataForObject(@ApiParam @NotNull Search search) {
        if (search == null) {
            throw new IllegalArgumentException("Search must not be null.");
        }
        final Map<String, QueryMetadata> queryMetadatas = StreamEx.of(search.queries()).toMap(Query::id, query -> queryEngine.parse(search, query));
        return SearchMetadata.create(queryMetadatas, Maps.uniqueIndex(search.parameters(), Parameter::name));
    }
}
