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
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_READ;

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
    @Path("settings/{username}")
    @ApiOperation("Retrieve Content Stream settings for specified user")
    public ContentStreamSettings getContentStreamUserSettings(
            @ApiParam(name = "username") @PathParam("username") String username,
            @Context UserContext userContext
    ) throws NotFoundException {
        if (userContext.getUser().getName().equals(username) || isPermitted(USERS_READ, username)) {
            return contentStreamService.getUserSettings(loadUser(username));
        }
        throw new ForbiddenException("Not allowed to view user " + username);
    }

    @PUT
    @Path("enable/{username}")
    @ApiOperation("Enable Content Stream for specified user")
    @AuditEvent(type = AuditEventTypes.CONTENT_STREAM_USER_SETTINGS_UPDATE)
    public ContentStreamSettings contentStreamEnable(
            @ApiParam(name = "username") @PathParam("username") String username,
            @Context UserContext userContext
    ) throws NotFoundException {
        if (userContext.getUser().getName().equals(username) || isPermitted(USERS_EDIT, username)) {
            return setStatus(username, true);
        }
        throw new ForbiddenException("Not allowed to edit user " + username);
    }

    @PUT
    @Path("disable/{username}")
    @ApiOperation("Disable Content Stream for specified user")
    @AuditEvent(type = AuditEventTypes.CONTENT_STREAM_USER_SETTINGS_UPDATE)
    public ContentStreamSettings contentStreamDisable(
            @ApiParam(name = "username") @PathParam("username") String username,
            @Context UserContext userContext
    ) throws NotFoundException {
        if (userContext.getUser().getName().equals(username) || isPermitted(USERS_EDIT, username)) {
            return setStatus(username, false);
        }
        throw new ForbiddenException("Not allowed to edit user " + username);
    }

    @PUT
    @Path("topics/{username}")
    @ApiOperation("Update Content Stream topic list for specified user")
    @AuditEvent(type = AuditEventTypes.CONTENT_STREAM_USER_SETTINGS_UPDATE)
    public ContentStreamSettings saveContentStreamUserTopics(
            @ApiParam(name = "username") @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "Content Stream topics for the specified user.", required = true)
            @Valid @NotNull List<String> topicList,
            @Context UserContext userContext
    ) throws NotFoundException {
        if (userContext.getUser().getName().equals(username) || isPermitted(USERS_EDIT, username)) {
            final User user = loadUser(username);
            ContentStreamSettings contentStreamSettings = contentStreamService.getUserSettings(user);
            ContentStreamSettings newSettings = ContentStreamSettings.builder()
                    .contentStreamEnabled(contentStreamSettings.contentStreamEnabled())
                    .topics(topicList)
                    .build();
            contentStreamService.saveUserSettings(user, newSettings);
            return newSettings;
        }
        throw new ForbiddenException("Not allowed to edit user " + username);
    }

    private ContentStreamSettings setStatus(String userName, boolean isEnabled) throws NotFoundException {
        final User user = loadUser(userName);
        ContentStreamSettings contentStreamSettings = contentStreamService.getUserSettings(user);
        ContentStreamSettings newSettings = ContentStreamSettings.builder()
                .contentStreamEnabled(isEnabled)
                .topics(contentStreamSettings.topics())
                .build();
        contentStreamService.saveUserSettings(user, newSettings);
        return newSettings;
    }

    private User loadUser(String userName) throws NotFoundException {
        final User user = userService.load(userName);
        if (user == null) {
            throw new org.graylog2.database.NotFoundException("User " + userName + " has not been found.");
        }
        return user;
    }
}
