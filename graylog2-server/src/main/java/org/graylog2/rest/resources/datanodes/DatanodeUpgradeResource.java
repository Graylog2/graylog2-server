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
package org.graylog2.rest.resources.datanodes;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog2.datanode.DatanodeUpgradeService;
import org.graylog2.datanode.DatanodeUpgradeStatus;
import org.graylog2.shared.security.RestPermissions;

@RequiresAuthentication
@Api(value = "DatanodeUpgrade", description = "Endpoint for support of rolling upgrade of data nodes")
@Produces(MediaType.APPLICATION_JSON)
@Path("/datanodes/upgrade")
public class DatanodeUpgradeResource {

    private final DatanodeUpgradeService upgradeService;

    @Inject
    public DatanodeUpgradeResource(DatanodeUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }


    @GET
    @Path("status")
    @ApiOperation("Display existing cluster configuration")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public DatanodeUpgradeStatus status() {
        return upgradeService.status();
    }

    @POST
    @Path("/stop-sync")
    @ApiOperation("Display existing cluster configuration")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public FlushResponse stopSync() {
        return upgradeService.stopSync();
    }

    @POST
    @Path("/start-sync")
    @ApiOperation("Display existing cluster configuration")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public FlushResponse startSync  () {
        return upgradeService.startSync();
    }
}
