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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.RestrictToMaster;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Deflector", description = "Index deflector management")
@Path("/system/deflector")
public class DeflectorResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeflectorResource.class);

    private final IndexSetRegistry indexSetRegistry;
    private final ActivityWriter activityWriter;

    @Inject
    public DeflectorResource(IndexSetRegistry indexSetRegistry,
                             ActivityWriter activityWriter) {
        this.indexSetRegistry = indexSetRegistry;
        this.activityWriter = activityWriter;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current deflector status")
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
    @ApiOperation(value = "Get current deflector status in index set")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public DeflectorSummary deflector(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) throws TooManyAliasesException {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        return DeflectorSummary.create(indexSet.isUp(), indexSet.getActiveWriteIndex());
    }

    @POST
    @Timed
    @ApiOperation(value = "Cycle deflector to new/next index")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/cycle")
    @RestrictToMaster
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
    @ApiOperation(value = "Cycle deflector to new/next index in index set")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/{indexSetId}/cycle")
    @RestrictToMaster
    @AuditEvent(type = AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START)
    public void cycle(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        checkCycle(indexSet);

        final String msg = "Cycling deflector for index set <" + indexSetId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DeflectorResource.class));

        indexSet.cycle();
    }

    private void checkCycle(IndexSet indexSet) {
        if (!indexSet.getConfig().isWritable()) {
            final String id = indexSet.getConfig().id();
            final String title = indexSet.getConfig().title();
            throw new BadRequestException("Unable to cycle non-writable index set <" + id + "> (" + title + ")");
        }
    }
}
