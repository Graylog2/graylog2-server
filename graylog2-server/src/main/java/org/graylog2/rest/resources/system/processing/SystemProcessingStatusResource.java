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
package org.graylog2.rest.resources.system.processing;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.system.processing.ProcessingStatusSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.processing.DBProcessingStatusService;
import org.graylog2.system.processing.ProcessingStatusRecorder;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "System/Processing/Status")
@Path("/system/processing/status")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
public class SystemProcessingStatusResource extends RestResource {
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final DBProcessingStatusService dbService;

    @Inject
    public SystemProcessingStatusResource(ProcessingStatusRecorder processingStatusRecorder,
                                          DBProcessingStatusService dbService) {
        this.processingStatusRecorder = processingStatusRecorder;
        this.dbService = dbService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get processing status summary from node")
    public ProcessingStatusSummary getStatus() {
        return ProcessingStatusSummary.of(processingStatusRecorder);
    }

    @GET
    @Path("/persisted")
    @Timed
    @ApiOperation(value = "Get persisted processing status summary from node")
    public ProcessingStatusSummary getPersistedStatus() {
        return dbService.get().map(ProcessingStatusSummary::of)
                .orElseThrow(() -> new NotFoundException("No processing status persisted yet"));
    }
}
