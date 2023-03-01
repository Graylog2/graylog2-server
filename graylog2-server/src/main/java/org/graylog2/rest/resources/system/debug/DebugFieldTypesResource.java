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
package org.graylog2.rest.resources.system.debug;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerPeriodical;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequiresAuthentication
@Api(value = "System/Debug/Field Types", description = "For triggering field type refreshs.")
@Path("/system/debug/field_types")
@Produces(MediaType.APPLICATION_JSON)
public class DebugFieldTypesResource {
    private final IndexFieldTypePollerPeriodical poller;

    public DebugFieldTypesResource(IndexFieldTypePollerPeriodical poller) {
        this.poller = poller;
    }

    @POST
    @Path("/refresh")
    @ApiOperation(value = "Get information about currently active stream router engine.")
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("Only used for tests.")
    public Response triggerFieldTypesRefresh() {
        this.poller.doRun();

        return Response.ok().build();
    }
}
