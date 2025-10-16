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
package org.graylog.plugins.views.startpage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.favorites.Favorite;
import org.graylog.plugins.views.startpage.lastOpened.LastOpened;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivity;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

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

import java.util.Optional;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@PublicCloudAPI
@Tag(name = "StartPage")
@Path("/startpage")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StartPageResource extends RestResource implements PluginRestResource {
    private final StartPageService service;

    @Inject
    public StartPageResource(StartPageService service) {
        this.service = service;
    }

    @GET
    @Path("/lastOpened")
    @Operation(summary = "Get the Last Opened Items for the Start Page for the user")
    public PaginatedResponse<LastOpened> getLastOpened(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                       @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("5") int perPage,
                                                       @Context SearchUser searchUser) {
        return service.findLastOpenedFor(searchUser, page, perPage);
    }

    @GET
    @Path("/recentActivity")
    @Operation(summary = "Get Recent Activities for the Start Page for the user")
    public PaginatedResponse<RecentActivity> getRecentActivity(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                               @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("5") int perPage,
                                                               @Context SearchUser searchUser) {
        return service.findRecentActivityFor(searchUser, page, perPage);
    }
}
