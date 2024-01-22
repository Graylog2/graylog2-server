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
package org.graylog.plugins.views.storage.migration;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.shared.security.RestPermissions;

@Path("/remote-reindex-migration")
@RequiresAuthentication
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "ReindexMigration", description = "Migrate data from existing cluster")
public class RemoteReindexResource {
    private final RemoteReindexingMigrationAdapter migrationService;

    @Inject
    public RemoteReindexResource(RemoteReindexingMigrationAdapter migrationService) {
        this.migrationService = migrationService;
    }

    @POST
    @Path("/remoteReindex")
    @NoAuditEvent("No Audit Event needed")
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "by remote reindexing", notes = "configure the host/credentials you want to use to migrate data from")
    public RemoteReindexResult migrate(@ApiParam(name = "remote configuration") @NotNull @Valid RemoteReindexRequest request) {
        return migrationService.start(request.hostname(), request.user(), request.password(), request.indices());
    }

    @GET
    @Path("/remoteReindex")
    @NoAuditEvent("No Audit Event needed")
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "status", notes = "status for a running migration")
    public RemoteReindexingMigrationAdapter.Status status() {
        return migrationService.status();
    }

}
