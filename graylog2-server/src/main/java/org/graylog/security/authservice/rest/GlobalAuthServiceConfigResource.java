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
package org.graylog.security.authservice.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.SecurityAuditEventTypes;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

@Path("/system/authentication/services/configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "System/Authentication/Services/Configuration", description = "Manage global authentication services configuration")
@RequiresAuthentication
public class GlobalAuthServiceConfigResource extends RestResource {
    private final GlobalAuthServiceConfig authServiceConfig;

    @Inject
    public GlobalAuthServiceConfigResource(GlobalAuthServiceConfig authServiceConfig) {
        this.authServiceConfig = authServiceConfig;
    }

    @GET
    @ApiOperation("Get global authentication services configuration")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_READ)
    public Response get() {
        return toResponse(authServiceConfig.getConfiguration());
    }

    @POST
    @ApiOperation("Update global authentication services configuration")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_EDIT)
    @AuditEvent(type = SecurityAuditEventTypes.AUTH_SERVICE_GLOBAL_CONFIG_UPDATE)
    public Response update(@ApiParam(name = "JSON body", required = true) @NotNull GlobalAuthServiceConfig.Data body) {
        return toResponse(authServiceConfig.updateConfiguration(body));
    }

    private Response toResponse(GlobalAuthServiceConfig.Data configuration) {
        return Response.ok(Collections.singletonMap("configuration", configuration)).build();
    }
}
