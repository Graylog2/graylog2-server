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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.security.UserContext;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.resources.entities.preferences.model.EntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesService;
import org.graylog2.shared.rest.PublicCloudAPI;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "EntityLists", description = "Entity Lists Preferences")
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
    @Operation(summary = "Create or update user preferences for certain entity list")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoAuditEvent("Audit logs are not stored for entity list preferences")
    public Response create(@RequestBody(required = true) EntityListPreferences entityListPreferences,
                           @Parameter(name = "entity_list_id", required = true) @PathParam("entity_list_id") @NotEmpty String entityListId,
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
    @Operation(summary = "Get preferences for user's entity list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get preferences for user's entity list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EntityListPreferences.class))),
            @ApiResponse(responseCode = "404", description = "Preferences not found.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public EntityListPreferences get(@Parameter(name = "entity_list_id", required = true) @PathParam("entity_list_id") @NotEmpty String entityListId,
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
