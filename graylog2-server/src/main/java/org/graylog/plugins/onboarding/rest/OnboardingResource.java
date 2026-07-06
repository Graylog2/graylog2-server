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
package org.graylog.plugins.onboarding.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.onboarding.OnboardingState;
import org.graylog.plugins.onboarding.OnboardingStatus;
import org.graylog.plugins.onboarding.audit.OnboardingAuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

@Tag(name = "Onboarding", description = "Manages the onboarding process state")
@Path("/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class OnboardingResource extends RestResource {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public OnboardingResource(final ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    @NoAuditEvent("Read-only operation")
    @Operation(summary = "Get current onboarding status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current onboarding status")
    })
    public OnboardingState get() {
        if (isPermitted(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)) {
            final OnboardingState status = clusterConfigService.get(OnboardingState.class);
            if (status != null) {
                return status;
            }
        }
        return new OnboardingState(OnboardingStatus.UNKNOWN);
    }

    @PUT
    @Path("/dismiss")
    @AuditEvent(type = OnboardingAuditEventTypes.ONBOARDING_DISMISSED)
    @Operation(summary = "Update onboarding status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated onboarding status"),
            @ApiResponse(responseCode = "400", description = "Bad request, illegal status value")
    })
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT)
    public void dismiss() {
        clusterConfigService.write(new OnboardingState(OnboardingStatus.DISMISSED));
    }
}
