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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.suggestions.EntitySuggestionResponse;
import org.graylog2.database.suggestions.EntitySuggestionService;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "EntitySuggestions")
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
    @Operation(summary = "Get a paginated list of suggested entities")
    public EntitySuggestionResponse getPage(@Parameter(name = "collection")
                                            @QueryParam("collection") String collection,
                                            @Parameter(name = "column")
                                            @QueryParam("column") @DefaultValue("title") String column,
                                            @Parameter(name = "page")
                                            @QueryParam("page") @DefaultValue("1") int page,
                                            @Parameter(name = "per_page")
                                            @QueryParam("per_page") @DefaultValue("10") int perPage,
                                            @Parameter(name = "query")
                                            @QueryParam("query") @DefaultValue("") String query) {

        return entitySuggestionService.suggest(collection, column, query, page, perPage, getSubject());
    }
}
