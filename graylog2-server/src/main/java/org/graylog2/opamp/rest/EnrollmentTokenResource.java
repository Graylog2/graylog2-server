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
package org.graylog2.opamp.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;

@Tag(name = "OpAMP Enrollment", description = "OpAMP agent enrollment management")
@Path("/opamp/enrollment-tokens")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EnrollmentTokenResource {

    private final EnrollmentTokenService enrollmentTokenService;

    @Inject
    public EnrollmentTokenResource(EnrollmentTokenService enrollmentTokenService) {
        this.enrollmentTokenService = enrollmentTokenService;
    }

    // TODO: Add @AuditEvent for security audit logging of token creation
    @NoAuditEvent("TODO")
    @POST
    @Operation(summary = "Create an enrollment token for OpAMP agents")
    // TODO: Replace with proper OpAMP permissions (e.g., opamp:enrollment_tokens:create)
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_CREATE)
    public EnrollmentTokenResponse createToken(
            @RequestBody(description = "Enrollment token creation request")
            @Valid @NotNull CreateEnrollmentTokenRequest request) {
        return enrollmentTokenService.createToken(request);
    }
}
