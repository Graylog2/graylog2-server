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
package org.graylog.plugins.views.favorites;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.PublicCloudAPI;

import java.util.Optional;

@PublicCloudAPI
@Tag(name = "Favorites")
@Path("/favorites")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FavoritesResource {
    final private FavoritesService favoritesService;

    @Inject
    public FavoritesResource(final FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    @GET
    @Operation(summary = "Get the Favorites for the Start Page for the user")
    public PaginatedResponse<Favorite> getFavoriteItems(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") @Min(1) int page,
                                                        @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("5") @Min(1) int perPage,
                                                        @Parameter(name = "type") @QueryParam("type") Optional<String> type,
                                                        @Context SearchUser searchUser) {
        return favoritesService.findFavoritesFor(searchUser, type, page, perPage);
    }

    @PUT
    @Path("/{grn}")
    @Operation(summary = "Add an item for inclusion on the Start Page for the user")
    @AuditEvent(type = ViewsAuditEventTypes.DYNAMIC_STARTUP_PAGE_ADD_FAVORITE_ITEM)
    public void addItemToFavorites(@Parameter(name = "grn", required = true) @PathParam("grn") @NotEmpty String grn, @Context SearchUser searchUser) {
        favoritesService.addFavoriteItemFor(grn, searchUser);
    }

    @DELETE
    @Path("/{grn}")
    @Operation(summary = "Remove an item from inclusion on the Start Page for the user")
    @AuditEvent(type = ViewsAuditEventTypes.DYNAMIC_STARTUP_PAGE_REMOVE_FAVORITE_ITEM)
    public void removeItemFromFavorites(@Parameter(name = "grn", required = true) @PathParam("grn") @NotEmpty String grn, @Context SearchUser searchUser) {
        favoritesService.removeFavoriteItemFor(grn, searchUser);
    }

}
