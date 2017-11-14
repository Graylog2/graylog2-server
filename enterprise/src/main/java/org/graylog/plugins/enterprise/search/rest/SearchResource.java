package org.graylog.plugins.enterprise.search.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.db.SearchDbService;
import org.graylog.plugins.enterprise.search.db.SearchJobService;
import org.graylog.plugins.enterprise.search.engine.QueryEngine;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

// TODO permission system
@Api(value = "Enterprise/Search", description = "Searching")
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SearchResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    private static final String BASE_PATH = "plugins/org.graylog.plugins.enterprise/search";

    private final QueryEngine queryEngine;
    private final SearchDbService searchDbService;
    private final SearchJobService searchJobService;
    private final ObjectMapper objectMapper;

    @Inject
    public SearchResource(QueryEngine queryEngine,
                          SearchDbService searchDbService,
                          SearchJobService searchJobService,
                          ObjectMapper objectMapper) {
        this.queryEngine = queryEngine;
        this.searchDbService = searchDbService;
        this.searchJobService = searchJobService;
        this.objectMapper = objectMapper;
    }

    @POST
    @ApiOperation(value = "Create a search query", response = Search.class, code = 201)
    public Response createSearch(@ApiParam Search search) {

        final Search saved = searchDbService.save(search);
        if (saved == null || saved.id() == null) {
            return Response.serverError().build();
        }
        LOG.info("Created new search object {}", saved.id());
//        final Query annotated = saved.withInfo(queryEngine.parse(saved));
        Search annotated = saved;
        //noinspection ConstantConditions
        return Response.created(URI.create(annotated.id())).entity(annotated).build();
    }

    @GET
    @ApiOperation(value = "Retrieve a search query")
    @Path("{id}")
    public Search getSearch(@ApiParam(name = "id") @PathParam("id") String queryId) {
        return searchDbService.get(queryId)
//                .map(query -> query.withInfo(queryEngine.parse(query)))
                .orElseThrow(() -> new NotFoundException("No such search query " + queryId));
    }

    @GET
    @ApiOperation(value = "Get all current search queries in the system")
    public List<Search> getAllSearches() {
        // TODO should be paginated and limited to own (or visible queries)
        return searchDbService.streamAll()
//                .map(query -> query.withInfo(queryEngine.parse(query)))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Execute the referenced search query asynchronously",
            notes = "Starts a new search, irrespective whether or not another is already running")
    @Path("{id}/execute")
    public Response executeQuery(@Context UriInfo uriInfo,
                                 @ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam Map<String, Object> executionState) {
        Search search = getSearch(id);
        search = search.applyExecutionState(objectMapper, executionState);

        final SearchJob searchJob = searchJobService.create(search);

        final SearchJob runningSearchJob = queryEngine.execute(searchJob);

        return Response.created(URI.create(BASE_PATH + "/status/" + runningSearchJob.getId()))
                .entity(runningSearchJob)
                .build();
    }


    @GET
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    public SearchJob jobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId) {
        final SearchJob searchJob = searchJobService.load(jobId).orElseThrow(NotFoundException::new);
        try {
            // force a "conditional join", to catch fast responses without having to poll
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(),5, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException ignore) {
        }
        return searchJob;
    }

    @POST
    @ApiOperation(value = "Execute a new synchronous search", notes = "Executes a new search and waits for its result")
    @Path("sync")
    public SearchJob executeSyncJob(@ApiParam Search search,
                                    @ApiParam(name = "timeout", defaultValue = "60000")
                                    @QueryParam("timeout") @DefaultValue("60000") long timeout) {
        final SearchJob searchJob = queryEngine.execute(searchJobService.create(search));

        try {
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new InternalServerErrorException("Timeout while executing search job");
        }

        return searchJob;
    }
}
