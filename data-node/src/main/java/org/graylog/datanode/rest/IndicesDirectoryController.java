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
package org.graylog.datanode.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.filesystem.index.OpensearchDataDirCompatibility;
import org.graylog.datanode.filesystem.index.OpensearchDataDirCompatibilityService;

@Path("/indices-directory")
@Produces(MediaType.APPLICATION_JSON)
public class IndicesDirectoryController {

    private final Configuration configuration;
    private final OpensearchDataDirCompatibilityService compatibilityService;

    @Inject
    public IndicesDirectoryController(Configuration configuration, OpensearchDataDirCompatibilityService compatibilityService) {
        this.configuration = configuration;
        this.compatibilityService = compatibilityService;
    }

    @GET
    @Path("compatibility")
    public CompatibilityResult status() {
        final OpensearchDataDirCompatibility compatibility = compatibilityService.check();
        return new CompatibilityResult(configuration.getHostname(), compatibility.opensearchVersion(),
                compatibility.info(), compatibility.errors(), compatibility.warnings());
    }
}
