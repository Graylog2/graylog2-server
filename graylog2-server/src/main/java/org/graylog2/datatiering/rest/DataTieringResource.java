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
package org.graylog2.datatiering.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.datatiering.DataTieringOrchestrator;
import org.graylog2.datatiering.DataTieringState;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/IndexSets/DataTiering", description = "Data Tiering")
@Path("/system/indices/index_sets/data_tiering")
@Produces(MediaType.APPLICATION_JSON)
public class DataTieringResource extends RestResource {

    private final DataTieringOrchestrator dataTieringOrchestrator;

    @Inject
    public DataTieringResource(DataTieringOrchestrator dataTieringOrchestrator) {
        this.dataTieringOrchestrator = dataTieringOrchestrator;
    }

    @GET
    @Path("state")
    @Timed
    @ApiOperation(value = "Get state of data tiering")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
    })
    public DataTieringState state() {
        return dataTieringOrchestrator.getState();
    }
}
