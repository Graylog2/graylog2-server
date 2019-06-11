package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import one.util.streamex.StreamEx;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.audit.EnterpriseAuditEventTypes;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchMetadata;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog.plugins.views.audit.EnterpriseAuditEventTypes;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchMetadata;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.stream.Collectors.toSet;

// TODO permission system
@Api(value = "Enterprise/Search", description = "Searching")
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@RequiresPermissions(EnterpriseSearchRestPermissions.EXTENDEDSEARCH_USE)
public class SearchResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

    private static final String BASE_PATH = "plugins/org.graylog.plugins.enterprise/search";

    private final QueryEngine queryEngine;
    private final SearchDbService searchDbService;
    private final SearchJobService searchJobService;
    private final ObjectMapper objectMapper;
    private final StreamService streamService;
    private final Map<String, PluginMetaData> providedCapabilities;

    @Inject
    public SearchResource(QueryEngine queryEngine,
                          SearchDbService searchDbService,
                          SearchJobService searchJobService,
                          ObjectMapper objectMapper,
                          StreamService streamService,
                          Map<String, PluginMetaData> providedCapabilities) {
        this.queryEngine = queryEngine;
        this.searchDbService = searchDbService;
        this.searchJobService = searchJobService;
        this.objectMapper = objectMapper;
        this.streamService = streamService;
        this.providedCapabilities = providedCapabilities;
    }

    @VisibleForTesting
    boolean isOwnerOfSearch(Search search, String username) {
        return search.owner()
                .map(owner -> owner.equals(username))
                .orElse(true);
    }

    @POST
    @ApiOperation(value = "Create a search query", response = Search.class, code = 201)
    @RequiresPermissions(EnterpriseSearchRestPermissions.EXTENDEDSEARCH_CREATE)
    @AuditEvent(type = EnterpriseAuditEventTypes.SEARCH_CREATE)
    public Response createSearch(@ApiParam Search search) {
        final String username = getCurrentUser() != null ? getCurrentUser().getName() : null;
        final boolean isAdmin = getCurrentUser() != null && (getCurrentUser().isLocalAdmin() || isPermitted("*"));
        final Optional<Search> previous = searchDbService.get(search.id());
        if (!isAdmin && !previous.map(existingSearch -> isOwnerOfSearch(existingSearch, username)).orElse(true)) {
            throw new ForbiddenException("Unable to update search with id <" + search.id() + ">, already exists and user is not permitted to overwrite it.");
        }

        final Search saved = searchDbService.save(search.toBuilder().owner(username).build());
        if (saved == null || saved.id() == null) {
            return Response.serverError().build();
        }
        LOG.debug("Created new search object {}", saved.id());
        return Response.created(URI.create(Objects.requireNonNull(saved.id()))).entity(saved).build();
    }

    @GET
    @ApiOperation(value = "Retrieve a search query")
    @Path("{id}")
    public Search getSearch(@ApiParam(name = "id") @PathParam("id") String searchId) {
        return searchDbService.getForUser(searchId, getCurrentUser(), viewId -> isPermitted(EnterpriseSearchRestPermissions.VIEW_READ, viewId))
                .orElseThrow(() -> new NotFoundException("No such search " + searchId));
    }

    @GET
    @ApiOperation(value = "Get all current search queries in the system")
    public List<Search> getAllSearches() {
        // TODO should be paginated and limited to own (or visible queries)
        // make sure we close the iterator properly
        try (Stream<Search> searchStream = searchDbService.streamAll()) {
            return searchStream.collect(Collectors.toList());
        }
    }

    private void checkUserIsPermittedToSeeStreams(Set<String> streamIds) {
        final Set<String> forbiddenStreams = streamIds.stream()
                .filter(streamId -> !isPermitted(RestPermissions.STREAMS_READ, streamId))
                .collect(Collectors.toSet());

        // We are not using `checkPermission` and throwing the exception ourselves to avoid leaking stream ids.
        if (!forbiddenStreams.isEmpty()) {
            LOG.warn("Not executing search, it is referencing inaccessible streams: [" + Joiner.on(',').join(forbiddenStreams) + "]");
            throwStreamAccessForbiddenException();
        }
    }

    private void throwStreamAccessForbiddenException() {
        throw new ForbiddenException("The search is referencing at least one stream you are not permitted to see.");
    }

    @POST
    @ApiOperation(value = "Execute the referenced search query asynchronously",
            notes = "Starts a new search, irrespective whether or not another is already running")
    @Path("{id}/execute")
    @AuditEvent(type = EnterpriseAuditEventTypes.SEARCH_JOB_CREATE)
    public Response executeQuery(@ApiParam(name = "id") @PathParam("id") String id,
                                 @ApiParam Map<String, Object> executionState) {
        Search search = getSearch(id);

        final Optional<Set<String>> usedStreamIds = search.queries().stream().map(Query::usedStreamIds).reduce(Sets::union);

        checkUserIsPermittedToSeeStreams(usedStreamIds.orElse(Collections.emptySet()));

        final boolean isAnyQueryWithoutStreams = search.queries()
                .stream()
                .anyMatch(query -> query.usedStreamIds().isEmpty());

        if (isAnyQueryWithoutStreams) {
            final Set<String> allAvailableStreamIds = availableStreamIds();

            if (allAvailableStreamIds.isEmpty()) {
                throwStreamAccessForbiddenException();
            }


            final ImmutableSet<Query> newQueries = search.queries().stream().map(query -> {
                if (query.usedStreamIds().isEmpty()) {
                    return query.toBuilder().filter(addStreamIdsToFilter(allAvailableStreamIds, query.filter())).build();
                }
                return query;
            }).collect(ImmutableSet.toImmutableSet());

            search = search.toBuilder().queries(newQueries).build();
        }

        final Map<String, PluginMetadataSummary> missingRequirements = missingRequirementsForEach(search);
        if (!missingRequirements.isEmpty()) {
            final Map<String, Object> error = ImmutableMap.of(
                    "error", "Unable to execute this search, the following capabilities are missing:",
                    "missing", missingRequirements
            );
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        search = search.applyExecutionState(objectMapper, firstNonNull(executionState, Collections.emptyMap()));

        final String username = getCurrentUser() != null ? getCurrentUser().getName() : null;

        final SearchJob searchJob = searchJobService.create(search, username);

        final SearchJob runningSearchJob = queryEngine.execute(searchJob);

        return Response.created(URI.create(BASE_PATH + "/status/" + runningSearchJob.getId()))
                .entity(runningSearchJob)
                .build();
    }

    private Map<String, PluginMetadataSummary> missingRequirementsForEach(Search search) {
        return search.requires().entrySet().stream()
                .filter(entry -> !this.providedCapabilities.containsKey(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Filter addStreamIdsToFilter(Set<String> allAvailableStreamIds, Filter filter) {
        final Filter orFilter = filteringForStreamIds(allAvailableStreamIds);
        if (filter == null) {
            return orFilter;
        }
        return AndFilter.and(orFilter, filter);
    }


    private Set<String> availableStreamIds() {
        return streamService.loadAll().stream()
                .map(org.graylog2.plugin.streams.Stream::getId)
                .filter(streamId -> isPermitted(RestPermissions.STREAMS_READ, streamId))
                .collect(toSet());
    }

    private Filter filteringForStreamIds(Set<String> streamIds) {
        final Set<Filter> streamFilters = streamIds.stream()
                .map(StreamFilter::ofId)
                .collect(toSet());
        return OrFilter.builder()
                .filters(streamFilters)
                .build();
    }

    @GET
    @ApiOperation(value = "Retrieve the status of an executed query")
    @Path("status/{jobId}")
    public SearchJob jobStatus(@ApiParam(name = "jobId") @PathParam("jobId") String jobId) {
        final String username = getCurrentUser() != null ? getCurrentUser().getName() : null;
        final SearchJob searchJob = searchJobService.load(jobId, username).orElseThrow(NotFoundException::new);
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
    @AuditEvent(type = EnterpriseAuditEventTypes.SEARCH_EXECUTE)
    public SearchJob executeSyncJob(@ApiParam Search search,
                                    @ApiParam(name = "timeout", defaultValue = "60000")
                                    @QueryParam("timeout") @DefaultValue("60000") long timeout) {
        final String username = getCurrentUser() != null ? getCurrentUser().getName() : null;
        final SearchJob searchJob = queryEngine.execute(searchJobService.create(search, username));

        try {
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException  e) {
            LOG.error("Error executing search job <{}>", searchJob.getId(), e);
            throw new InternalServerErrorException("Error executing search job: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new InternalServerErrorException("Timeout while executing search job");
        } catch (Exception e) {
            LOG.error("Other error", e);
            throw e;
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
