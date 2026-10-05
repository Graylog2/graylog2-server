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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.rest.bulk.AuditParams;
import org.graylog2.rest.bulk.BulkExecutor;
import org.graylog2.rest.bulk.SequentialBulkExecutor;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.RestrictToLeader;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiresAuthentication
@Tag(name = "System/Deflector", description = "Index deflector management")
@Path("/system/deflector")
public class DeflectorResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeflectorResource.class);

    private final IndexSetRegistry indexSetRegistry;
    private final ActivityWriter activityWriter;
    private final BulkExecutor<IndexSet, UserContext> bulkCycleExecutor;

    @Inject
    public DeflectorResource(IndexSetRegistry indexSetRegistry,
                             ActivityWriter activityWriter,
                             AuditEventSender auditEventSender,
                             ObjectMapper objectMapper) {
        this.indexSetRegistry = indexSetRegistry;
        this.activityWriter = activityWriter;
        this.bulkCycleExecutor = new SequentialBulkExecutor<>(this::cycleInner, auditEventSender, objectMapper);
    }

    @GET
    @Timed
    @Operation(summary = "Get current deflector status")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public DeflectorSummary deprecatedDeflector() throws TooManyAliasesException {
        final IndexSet indexSet = indexSetRegistry.getDefault();
        return DeflectorSummary.create(indexSet.isUp(), indexSet.getActiveWriteIndex());
    }

    @GET
    @Timed
    @Path("/{indexSetId}")
    @Operation(summary = "Get current deflector status in index set")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public DeflectorSummary deflector(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId) throws TooManyAliasesException {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        return DeflectorSummary.create(indexSet.isUp(), indexSet.getActiveWriteIndex());
    }

    @POST
    @Timed
    @Operation(summary = "Cycle deflector to new/next index")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/cycle")
    @RestrictToLeader
    @AuditEvent(type = AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START)
    @Deprecated
    public void deprecatedCycle() {
        final IndexSet indexSet = indexSetRegistry.getDefault();

        checkCycle(indexSet);

        final String msg = "Cycling deflector for default index set <" + indexSet.getConfig().id() + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DeflectorResource.class));

        indexSet.cycle();
    }

    @POST
    @Timed
    @Operation(summary = "Cycle deflector to new/next index in index set")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/{indexSetId}/cycle")
    @RestrictToLeader
    @AuditEvent(type = AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START)
    public void cycle(@Parameter(name = "indexSetId") @PathParam("indexSetId") String indexSetId,
                      @Context UserContext userContext) throws TooManyAliasesException {
        cycleInner(indexSetId, userContext);
    }

    @POST
    @Path("/bulk_cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @Operation(summary = "Cycle deflectors of a bulk of index sets to new/next index")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycling successful",
                         content = @Content(schema = @Schema(implementation = BulkOperationResponse.class)))
    })
    @NoAuditEvent("Audit events triggered manually")
    public Response bulkCycle(@Parameter(name = "Entities to cycle", required = true) final BulkOperationRequest bulkOperationRequest,
                              @Context final UserContext userContext) {

        final BulkOperationResponse response = bulkCycleExecutor.executeBulkOperation(
                bulkOperationRequest,
                userContext,
                new AuditParams(AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START, "indexSetId", IndexSet.class));

        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    private IndexSet cycleInner(String indexSetId, UserContext userContext) {
        checkPermission(RestPermissions.DEFLECTOR_CYCLE);
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        checkCycle(indexSet);

        final String msg = "Cycling deflector for index set <" + indexSetId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DeflectorResource.class));

        indexSet.cycle();
        return indexSet;
    }

    private void checkCycle(IndexSet indexSet) {
        if (!indexSet.getConfig().isWritable()) {
            final String id = indexSet.getConfig().id();
            final String title = indexSet.getConfig().title();
            throw new BadRequestException("Unable to cycle non-writable index set <" + id + "> (" + title + ")");
        }
    }
}
