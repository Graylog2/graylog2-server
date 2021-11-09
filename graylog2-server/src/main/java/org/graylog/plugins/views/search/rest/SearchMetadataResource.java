package org.graylog.plugins.views.search.rest;

import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import one.util.streamex.StreamEx;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchMetadata;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Api(value = "Search/Metadata")
@Path("/views/search/metadata")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SearchMetadataResource extends RestResource implements PluginRestResource {
    private final QueryEngine queryEngine;
    private final SearchDomain searchDomain;

    @Inject
    public SearchMetadataResource(QueryEngine queryEngine,
                                  SearchDomain searchDomain) {
        this.queryEngine = queryEngine;
        this.searchDomain = searchDomain;
    }

    @GET
    @ApiOperation(value = "Metadata for the given Search object", notes = "Used for already persisted search objects")
    @Path("{searchId}")
    public SearchMetadata metadata(@ApiParam("searchId") @PathParam("searchId") String searchId, @Context SearchUser searchUser) {
        final Search search = searchDomain.getForUser(searchId, searchUser)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));
        return metadataForObject(search);
    }

    @POST
    @ApiOperation(value = "Metadata for the posted Search object", notes = "Intended for search objects that aren't yet persisted (e.g. for validation or interactive purposes)")
    @NoAuditEvent("Only returning metadata for given search, not changing any data")
    public SearchMetadata metadataForObject(@ApiParam @NotNull Search search) {
        if (search == null) {
            throw new IllegalArgumentException("Search must not be null.");
        }
        final Map<String, QueryMetadata> queryMetadatas = StreamEx.of(search.queries()).toMap(Query::id, query -> queryEngine.parse(search, query));
        return SearchMetadata.create(queryMetadatas, Maps.uniqueIndex(search.parameters(), Parameter::name));
    }
}
