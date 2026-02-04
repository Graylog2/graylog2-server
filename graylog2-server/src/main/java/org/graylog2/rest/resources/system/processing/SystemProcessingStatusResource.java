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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.system.processing.ProcessingStatusSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.processing.DBProcessingStatusService;
import org.graylog2.system.processing.ProcessingStatusRecorder;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "System/Processing/Status")
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
    @Operation(summary = "Get processing status summary from node")
    public ProcessingStatusSummary getStatus() {
        return ProcessingStatusSummary.of(processingStatusRecorder);
    }

    @GET
    @Path("/persisted")
    @Timed
    @Operation(summary = "Get persisted processing status summary from node")
    public ProcessingStatusSummary getPersistedStatus() {
        return dbService.get().map(ProcessingStatusSummary::of)
                .orElseThrow(() -> new NotFoundException("No processing status persisted yet"));
    }
}
