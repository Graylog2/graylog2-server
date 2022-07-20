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
package org.graylog2.database.entities;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Api(value = "EntityScope", description = "Provide a list of available Entity Scopes")
@Produces(MediaType.APPLICATION_JSON)
@Path("/entity_scopes")
@RequiresAuthentication
public class EntityScopeResource extends RestResource {

    private final EntityScopeService entityScopeService;

    @Inject
    public EntityScopeResource(EntityScopeService entityScopeService) {
        this.entityScopeService = entityScopeService;
    }

    @ApiOperation("Generate a mapping of available Entity Scopes")
    @GET
    public EntityScopes getAllEntityScopes() {

        Map<String, EntityScopeResponse> scopes = entityScopeService.getEntityScopes()
                .stream()
                .collect(Collectors.toMap(e -> e.getName().toUpperCase(Locale.ROOT), EntityScopeResponse::of));

        return EntityScopes.create(scopes);
    }
}
