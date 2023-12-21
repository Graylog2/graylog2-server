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
package org.graylog2.bootstrap.preflight.web.resources;

import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.ConfigurationStatus;
import org.graylog2.bootstrap.preflight.PreflightConfig;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.plugin.Version;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path(PreflightConstants.API_PREFIX + "status")
@Produces(MediaType.APPLICATION_JSON)
public class PreflightStatusResource {

    private final Version version = Version.CURRENT_CLASSPATH;
    private final PreflightConfigService preflightConfigService;

    @Inject
    public PreflightStatusResource(PreflightConfigService preflightConfigService) {
        this.preflightConfigService = preflightConfigService;
    }

    @GET
    public ConfigurationStatus status() {
        return new ConfigurationStatus(version.toString());
    }

    @NoAuditEvent("No audit event yet")
    @POST
    @Path("/finish-config")
    public PreflightConfig finishConfig() {
        return preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
    }

    @NoAuditEvent("No audit event yet")
    @POST
    @Path("/skip-config")
    public PreflightConfig skipConfig() {
        return preflightConfigService.setConfigResult(PreflightConfigResult.SKIPPED);
    }
}
