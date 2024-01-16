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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.querystrings.LastUsedQueryStringsService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Query Strings", tags = {CLOUD_VISIBLE})
@Path("/search/query_strings")
public class QueryStringsResource extends RestResource implements PluginRestResource {
    private final LastUsedQueryStringsService lastUsedQueryStringsService;

    @Inject
    public QueryStringsResource(LastUsedQueryStringsService lastUsedQueryStringsService) {
        this.lastUsedQueryStringsService = lastUsedQueryStringsService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Register a query string used")
    @NoAuditEvent("No audit event needed for this operation")
    public Response queryStringUsed(@ApiParam(name = "queryStringRequest") @Valid @NotNull final QueryStringUsedDTO queryStringUsed,
                                    @Context final SearchUser searchUser) {
        this.lastUsedQueryStringsService.save(searchUser.getUser(), queryStringUsed.queryString());
        return Response.noContent().build();
    }

}
