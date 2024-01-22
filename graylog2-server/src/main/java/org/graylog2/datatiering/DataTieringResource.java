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
package org.graylog2.datatiering;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;

@Api(value = "DataTiering", description = "Data tiering management")
@Path("/datatiering")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class DataTieringResource extends RestResource {

    private final DataTieringOrchestrator dataTieringOrchestrator;

    @Inject
    public DataTieringResource(DataTieringOrchestrator dataTieringOrchestrator) {
        this.dataTieringOrchestrator = dataTieringOrchestrator;
    }

    @GET
    @Path("default_config")
    @ApiOperation(value = "Get default configuration.")
    @Produces(MediaType.APPLICATION_JSON)
    public DataTieringConfig defaultConfig() {
        return dataTieringOrchestrator.defaultConfig();
    }
}
