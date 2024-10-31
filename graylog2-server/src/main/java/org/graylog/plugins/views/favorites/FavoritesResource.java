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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.grn.GRN;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.rest.models.PaginatedResponse;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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

import java.util.Optional;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Favorites", tags = {CLOUD_VISIBLE})
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
    @ApiOperation("Get the Favorites for the Start Page for the user")
    public PaginatedResponse<Favorite> getFavoriteItems(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("5") int perPage,
                                                        @ApiParam(name = "type") @QueryParam("type") Optional<String> type,
                                                        @Context SearchUser searchUser) {
        return favoritesService.findFavoritesFor(searchUser, type, page, perPage);
    }

    @PUT
    @Path("/{grn}")
    @ApiOperation("Add an item for inclusion on the Start Page for the user")
    @AuditEvent(type = ViewsAuditEventTypes.DYNAMIC_STARTUP_PAGE_ADD_FAVORITE_ITEM)
    public void addItemToFavorites(@ApiParam(name = "grn", required = true) @PathParam("grn") @NotEmpty String grn, @Context SearchUser searchUser) {
        favoritesService.addFavoriteItemFor(grn, searchUser);
    }

    @DELETE
    @Path("/{grn}")
    @ApiOperation("Remove an item from inclusion on the Start Page for the user")
    @AuditEvent(type = ViewsAuditEventTypes.DYNAMIC_STARTUP_PAGE_REMOVE_FAVORITE_ITEM)
    public void removeItemFromFavorites(@ApiParam(name = "grn", required = true) @PathParam("grn") @NotEmpty String grn, @Context SearchUser searchUser) {
        favoritesService.removeFavoriteItemFor(grn, searchUser);
    }

}
