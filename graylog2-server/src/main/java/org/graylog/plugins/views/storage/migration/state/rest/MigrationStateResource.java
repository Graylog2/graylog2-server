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
package org.graylog.plugins.views.storage.migration.state.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.shared.security.RestPermissions;

@Path("/migration")
@RequiresAuthentication
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Migration", description = "Resource for managing migration to datanode from open/elasticsearch")
public class MigrationStateResource {

    private final MigrationStateMachine stateMachine;

    @Inject
    public MigrationStateResource(MigrationStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @POST
    @Path("/trigger")
    @NoAuditEvent("No Audit Event needed") // TODO: do we need audit log here?
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "trigger migration step")
    public CurrentStateInformation migrate(@ApiParam(name = "request") @NotNull MigrationStepRequest request) {
        final MigrationState newState = stateMachine.trigger(request.step(), request.args());
        return new CurrentStateInformation(newState, stateMachine.nextSteps());
    }

    @GET
    @Path("/state")
    @NoAuditEvent("No Audit Event needed")
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "Migration status", notes = "Current status of the datanode migration")
    public CurrentStateInformation status() {
        return new CurrentStateInformation(stateMachine.getState(), stateMachine.nextSteps());
    }

    @GET
    @Path("/serialize")
    @NoAuditEvent("No Audit Event needed")
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Serialize", notes = "Serialize migration graph as graphviz source")
    public String serialize() {
        // you can use https://dreampuf.github.io/GraphvizOnline/ to vizualize the result
        return stateMachine.serialize();
    }
}
