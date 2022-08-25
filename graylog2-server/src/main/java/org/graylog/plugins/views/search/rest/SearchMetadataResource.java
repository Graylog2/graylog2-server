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

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Search/Metadata", tags = {CLOUD_VISIBLE})
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
        final SearchDTO search = searchDomain.getForUser(searchId, searchUser)
                .map(SearchDTO::fromSearch)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));
        return metadataForObject(search);
    }

    @POST
    @ApiOperation(value = "Metadata for the posted Search object", notes = "Intended for search objects that aren't yet persisted (e.g. for validation or interactive purposes)")
    @NoAuditEvent("Only returning metadata for given search, not changing any data")
    public SearchMetadata metadataForObject(@ApiParam @NotNull(message = "Search body is mandatory") SearchDTO searchDTO) {
        if (searchDTO == null) {
            throw new IllegalArgumentException("Search must not be null.");
        }
        final Search search = searchDTO.toSearch();
        final Map<String, QueryMetadata> queryMetadatas = StreamEx.of(search.queries()).toMap(Query::id, query -> queryEngine.parse(search, query));
        return SearchMetadata.create(queryMetadatas, Maps.uniqueIndex(search.parameters(), Parameter::name));
    }
}
