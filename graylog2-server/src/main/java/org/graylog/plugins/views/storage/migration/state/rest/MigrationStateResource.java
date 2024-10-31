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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.storage.migration.state.actions.TrafficSnapshot;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.security.RestPermissions;

@Path("/migration")
@RequiresAuthentication
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Migration", description = "Resource for managing migration to datanode from open/elasticsearch")
public class MigrationStateResource {

    private final MigrationStateMachine stateMachine;
    private final KafkaJournalConfiguration journalConfiguration;

    @Inject
    public MigrationStateResource(MigrationStateMachine stateMachine, @Context HttpHeaders httpHeaders, KafkaJournalConfiguration journalConfiguration) {
        this.stateMachine = stateMachine;
        this.journalConfiguration = journalConfiguration;
        this.stateMachine.getContext().addExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY, ProxiedResource.authenticationToken(httpHeaders));
    }

    @POST
    @Path("/trigger")
    @NoAuditEvent("No Audit Event needed") // TODO: do we need audit log here?
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "trigger migration step")
    public Response trigger(@ApiParam(name = "request") @NotNull MigrationStepRequest request) {
        final CurrentStateInformation newState = stateMachine.trigger(request.step(), request.args());
        Response.ResponseBuilder response = newState.hasErrors() ? Response.serverError() : Response.ok();
        return response.entity(newState)
                .build();
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
        // you can use https://dreampuf.github.io/GraphvizOnline/ to visualize the result
        return stateMachine.serialize();
    }

    @DELETE
    @Path("/state")
    @NoAuditEvent("No Audit Event needed") // TODO: do we need audit log here?
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "Reset the whole migration to the first step, start over")
    public CurrentStateInformation resetState() {
        stateMachine.reset();
        return new CurrentStateInformation(stateMachine.getState(), stateMachine.nextSteps());
    }

    @GET
    @Path("/journalestimate")
    @NoAuditEvent("No audit event needed")
    @RequiresPermissions(RestPermissions.DATANODE_MIGRATION)
    @ApiOperation(value = "Get journal size estimate (bytes/minute)")
    public JournalEstimate getJournalEstimate() {
        long bytesPerMinute = stateMachine.getContext()
                .getExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE, Long.class)
                .orElse(0L);
        long journalSize = journalConfiguration.getMessageJournalMaxSize().toBytes();
        long maxDowntimeMinutes = (bytesPerMinute != 0) ? Math.floorDiv(journalSize, bytesPerMinute) : journalSize;
        return new JournalEstimate(bytesPerMinute, journalSize, maxDowntimeMinutes);
    }
}
