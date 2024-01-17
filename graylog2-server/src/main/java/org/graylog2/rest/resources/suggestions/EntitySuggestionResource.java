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
package org.graylog2.rest.resources.suggestions;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.suggestions.EntitySuggestionResponse;
import org.graylog2.database.suggestions.EntitySuggestionService;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "EntitySuggestions", tags = {CLOUD_VISIBLE})
@Path("/entity_suggestions")
@Produces(MediaType.APPLICATION_JSON)
public class EntitySuggestionResource extends RestResource {

    private final EntitySuggestionService entitySuggestionService;

    @Inject
    public EntitySuggestionResource(final EntitySuggestionService entitySuggestionService) {
        this.entitySuggestionService = entitySuggestionService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a paginated list of suggested entities")
    public EntitySuggestionResponse getPage(@ApiParam(name = "collection")
                                            @QueryParam("collection") String collection,
                                            @ApiParam(name = "column")
                                            @QueryParam("column") @DefaultValue("title") String column,
                                            @ApiParam(name = "page")
                                            @QueryParam("page") @DefaultValue("1") int page,
                                            @ApiParam(name = "per_page")
                                            @QueryParam("per_page") @DefaultValue("10") int perPage,
                                            @ApiParam(name = "query")
                                            @QueryParam("query") @DefaultValue("") String query) {

        return entitySuggestionService.suggest(collection, column, query, page, perPage, getSubject());
    }
}
