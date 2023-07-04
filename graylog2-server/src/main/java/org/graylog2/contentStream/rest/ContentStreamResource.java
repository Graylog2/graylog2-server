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
package org.graylog2.contentStream.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "ContentStream", description = "Content Stream", tags = {CLOUD_VISIBLE})
@Path("/contentStream")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContentStreamResource extends RestResource {

    private final ContentStreamService contentStreamService;
    private final AuditEventSender auditEventSender;
    private final UserService userService;

    @Inject
    protected ContentStreamResource(ContentStreamService contentStreamService,
                                    AuditEventSender auditEventSender,
                                    UserService userService) {
        this.contentStreamService = contentStreamService;
        this.auditEventSender = auditEventSender;
        this.userService = userService;
    }

    @GET
    @Path("user/settings/{username}")
    @ApiOperation("Retrieve Content Stream settings for specified user")
    public ContentStreamUserSettings getContentStreamUserSettings(
            @ApiParam(name = "username") @PathParam("username") String username
    ) throws NotFoundException {
        final User user = userService.load(username);
        if (user == null) {
            throw new org.graylog2.database.NotFoundException("User " + username + " has not been found.");
        }
        return contentStreamService.getContentStreamUserSettings(user);
    }

    @PUT
    @Path("user/settings/{username}")
    @ApiOperation("Update Content Stream settings for specified user")
    @AuditEvent(type = AuditEventTypes.CONTENT_STREAM_USER_SETTINGS_UPDATE)
    public void saveContentStreamUserSettings(
            @ApiParam(name = "username") @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "The Content Stream settings to assign to the user.", required = true)
            @Valid @NotNull ContentStreamUserSettings contentStreamUserSettings) throws NotFoundException {

        final User user = userService.load(username);
        if (user == null) {
            throw new org.graylog2.database.NotFoundException("User " + username + " has not been found.");
        }
        contentStreamService.saveUserSettings(user, contentStreamUserSettings);
    }
}
