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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.system.responses.ReaderPermissionResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserConfiguration;

import java.util.Collection;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Permissions", description = "Retrieval of system permissions.", tags = {CLOUD_VISIBLE})
@Path("/system/permissions")
@Produces(APPLICATION_JSON)
public class PermissionsResource extends RestResource {
    private final Permissions permissions;
    private final UserService userService;
    private final ClusterConfigService configService;

    @Inject
    public PermissionsResource(final Permissions permissions, UserService userService, ClusterConfigService configService) {
        this.permissions = permissions;
        this.userService = userService;
        this.configService = configService;
    }

    @GET
    @Timed
    @RequiresGuest // turns off authentication for this action
    @ApiOperation(value = "Get all available user permissions.")
    public Map<String, Map<String, Collection<String>>> permissions() {
        return ImmutableMap.of("permissions", permissions.allPermissionsMap());
    }

    @GET
    @Timed
    @RequiresGuest
    @ApiOperation(value = "Get the initial permissions assigned to a reader account")
    @Path("reader/{username}")
    @Produces(APPLICATION_JSON)
    public ReaderPermissionResponse readerPermissions(
            @ApiParam(name = "username", required = true)
            @PathParam("username") String username) {
        final User user = userService.load(username);
        final boolean isExternal = user == null || user.isExternalUser();
        final UserConfiguration config = this.configService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
        final boolean isAllowedToCreateToken = this.permissions.isAllowedToCreateTokens(isExternal, config);

        return ReaderPermissionResponse.create(
                Ordering.natural().sortedCopy(permissions.userSelfEditPermissions(username, isAllowedToCreateToken)));
    }
}
