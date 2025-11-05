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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.test.AuthServiceBackendTestRequest;
import org.graylog.security.authservice.test.AuthServiceBackendTestService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

@Path("/system/authentication/services/test")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "System/Authentication/Services/Test", description = "Test authentication services")
@RequiresAuthentication
public class AuthServiceTestResource extends RestResource {
    private final AuthServiceBackendTestService testService;

    @Inject
    public AuthServiceTestResource(AuthServiceBackendTestService testService) {
        this.testService = testService;
    }

    @POST
    @Path("backend/connection")
    @Operation(summary = "Test authentication service backend connection")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_TEST_BACKEND_EXECUTE)
    @NoAuditEvent("Test resource - doesn't change any data")
    public Response backendConnection(@RequestBody(required = true) @NotNull AuthServiceBackendTestRequest request) {
        // We do NOT validate the backend configuration in the request here to make it possible to execute the
        // connection test with partial configuration data. This is needed in the UI when using a step-based wizard
        // and already wants to test the connection before having the user enter the complete configuration.
        return Response.ok(testService.testConnection(request)).build();
    }

    @POST
    @Path("backend/login")
    @Operation(summary = "Test authentication service backend login")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_TEST_BACKEND_EXECUTE)
    @NoAuditEvent("Test resource - doesn't change any data")
    public Response backendLogin(@RequestBody(required = true) @NotNull AuthServiceBackendTestRequest request) {
        validateConfig(request.backendConfiguration());

        return Response.ok(testService.testLogin(request)).build();
    }

    private void validateConfig(AuthServiceBackendDTO config) {
        final ValidationResult result = config.validate();

        if (result.failed()) {
            throw new ValidationFailureException(result);
        }
    }

}
