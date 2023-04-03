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
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.database.ValidationException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
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
    public PreflightConfig finishConfig() throws ValidationException {
        return preflightConfigService.saveConfiguration();
    }
}
