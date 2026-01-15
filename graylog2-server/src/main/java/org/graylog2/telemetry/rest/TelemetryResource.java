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
package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Map;

import static org.graylog2.audit.AuditEventTypes.TELEMETRY_USER_SETTINGS_UPDATE;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Telemetry", description = "Message inputs")
@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource extends RestResource {

    private final TelemetryService telemetryService;
    private final AuditEventSender auditEventSender;

    @Inject
    protected TelemetryResource(TelemetryService telemetryService,
                                AuditEventSender auditEventSender) {
        this.telemetryService = telemetryService;
        this.auditEventSender = auditEventSender;
    }

    @GET
    @Operation(summary = "Get telemetry information.")
    public ObjectNode get() {
        return telemetryService.getTelemetryResponse(getCurrentUserOrThrow());
    }

    @GET
    @Path("user/settings")
    @Operation(summary = "Retrieve a user's telemetry settings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns user settings", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Current user not found.")
    })
    public TelemetryUserSettings getTelemetryUserSettings() {
        return telemetryService.getTelemetryUserSettings(getCurrentUserOrThrow());
    }

    @PUT
    @Path("user/settings")
    @Operation(summary = "Update a user's telemetry settings.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Current user not found.")
    })
    @NoAuditEvent("Audit event is sent manually.")
    public void saveTelemetryUserSettings(@RequestBody(description = "The telemetry settings to assign to the user.", required = true)
                                          @Valid @NotNull TelemetryUserSettings telemetryUserSettings) {

        User currentUser = getCurrentUserOrThrow();
        telemetryService.saveUserSettings(currentUser, telemetryUserSettings);
        auditEventSender.success(
                AuditActor.user(currentUser.getName()),
                TELEMETRY_USER_SETTINGS_UPDATE,
                Map.of(
                        "telemetry_enabled", telemetryUserSettings.telemetryEnabled(),
                        "telemetry_permission_asked", telemetryUserSettings.telemetryPermissionAsked()
                )
        );
    }

    private User getCurrentUserOrThrow() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new NotFoundException("Couldn't find current user!");
        }
        return currentUser;
    }
}
