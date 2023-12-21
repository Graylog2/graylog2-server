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
package org.graylog2.rest.resources.entities.preferences;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.security.UserContext;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.resources.entities.preferences.model.EntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesService;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "EntityLists", description = "Entity Lists Preferences", tags = {CLOUD_VISIBLE})
@Path("/entitylists/preferences")
public class EntityListPreferencesResource {

    private final EntityListPreferencesService entityListPreferencesService;

    @Inject
    public EntityListPreferencesResource(final EntityListPreferencesService entityListPreferencesService) {
        this.entityListPreferencesService = entityListPreferencesService;
    }

    @POST
    @Path("/{entity_list_id}")
    @Timed
    @ApiOperation(value = "Create or update user preferences for certain entity list")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoAuditEvent("Audit logs are not stored for entity list preferences")
    public Response create(@ApiParam(name = "JSON body", required = true) EntityListPreferences entityListPreferences,
                           @ApiParam(name = "entity_list_id", required = true) @PathParam("entity_list_id") @NotEmpty String entityListId,
                           @Context UserContext userContext) throws ValidationException {

        final String currentUserId = userContext.getUserId();
        final StoredEntityListPreferencesId complexId = StoredEntityListPreferencesId.builder()
                .userId(currentUserId)
                .entityListId(entityListId)
                .build();
        final StoredEntityListPreferences storedPreferences = StoredEntityListPreferences.builder()
                .preferencesId(complexId)
                .preferences(entityListPreferences)
                .build();
        final boolean successful = entityListPreferencesService.save(storedPreferences);
        if (successful) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{entity_list_id}")
    @Timed
    @ApiOperation(value = "Get preferences for user's entity list", response = EntityListPreferences.class)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Preferences not found.")
    })
    public EntityListPreferences get(@ApiParam(name = "entity_list_id", required = true) @PathParam("entity_list_id") @NotEmpty String entityListId,
                                     @Context UserContext userContext) throws NotFoundException {

        final String currentUserId = userContext.getUserId();
        final StoredEntityListPreferencesId complexId = StoredEntityListPreferencesId.builder()
                .userId(currentUserId)
                .entityListId(entityListId)
                .build();
        final StoredEntityListPreferences entityListPreferences = entityListPreferencesService.get(complexId);
        if (entityListPreferences == null) {
            return null;
        }
        return entityListPreferences.preferences();
    }
}
