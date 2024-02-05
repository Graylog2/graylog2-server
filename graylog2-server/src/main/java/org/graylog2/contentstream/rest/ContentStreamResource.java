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
package org.graylog2.contentstream.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserService;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;

@RequiresAuthentication
@Api(value = "ContentStream", description = "Content Stream", tags = {CLOUD_VISIBLE})
@Path("/contentstream/")
@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public class ContentStreamResource extends RestResource {
    private final ContentStreamService contentStreamService;

    @Inject
    protected ContentStreamResource(ContentStreamService contentStreamService,
                                    UserService userService) {
        this.contentStreamService = contentStreamService;
        this.userService = userService;
    }

    @GET
    @Path("tags")
    @ApiOperation("Retrieve valid feed tags based on license")
    public List<String> getContentStreamTags() throws NotFoundException {
        return contentStreamService.getTags();
    }

    @GET
    @Path("settings/{username}")
    @ApiOperation("Retrieve Content Stream settings for specified user")
    public ContentStreamSettings getContentStreamUserSettings(
            @ApiParam(name = "username") @PathParam("username") String username
    ) throws NotFoundException {
        if (isPermitted(USERS_EDIT, username)) {
            return contentStreamService.getUserSettings(loadUser(username));
        }
        throw new ForbiddenException("Not allowed to view user " + username);
    }

    @PUT
    @Path("settings/{username}")
    @ApiOperation("Update Content Stream settings for specified user")
    @AuditEvent(type = AuditEventTypes.CONTENT_STREAM_USER_SETTINGS_UPDATE)
    public ContentStreamSettings setContentStreamUserSettings(
            @ApiParam(name = "username") @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "Content Stream settings for the specified user.", required = true)
            @Valid @NotNull ContentStreamSettings settings
    ) throws NotFoundException {
        if (isPermitted(USERS_EDIT, username)) {
            final User user = loadUser(username);
            ContentStreamSettings newSettings = ContentStreamSettings.builder()
                    .contentStreamEnabled(settings.contentStreamEnabled())
                    .releasesEnabled(settings.releasesEnabled())
                    .topics(settings.topics())
                    .build();
            contentStreamService.saveUserSettings(user, newSettings);
            return newSettings;
        }
        throw new ForbiddenException("Not allowed to edit user " + username);
    }

    private User loadUser(String userName) throws NotFoundException {
        final User user = userService.load(userName);
        if (user == null) {
            throw new org.graylog2.database.NotFoundException("User " + userName + " has not been found.");
        }
        return user;
    }
}
