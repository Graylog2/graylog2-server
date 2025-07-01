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
package org.graylog.integrations.dbconnector.api;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog.integrations.dbconnector.api.reponses.TimezoneResponse;
import org.graylog.integrations.dbconnector.api.requests.DBConnectorCreateInputRequest;
import org.graylog.integrations.dbconnector.api.requests.DBConnectorRequestImpl;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.resources.system.inputs.AbstractInputsResource;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTimeZone;

import java.util.Objects;
import java.util.Set;


/**
 * Web endpoints for the Database Connector integration.
 */
@Api(value = "DBConnector", description = "Database Connector integrations")
@Path("/dbconnector")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DBConnectorResource extends AbstractInputsResource implements PluginRestResource {

    private final DBConnectorDriver DBConnectorDriver;

    @Inject
    public DBConnectorResource(MessageInputFactory messageInputFactory, DBConnectorDriver DBConnectorDriver) {
        super(messageInputFactory.getAvailableInputs());
        this.DBConnectorDriver = DBConnectorDriver;
    }

    @GET
    @Timed
    @Path("/timezones")
    @ApiOperation(value = "Get all available timezones")
    public Response getTimezones() {
        Set<String> zoneIds = DateTimeZone.getAvailableIDs();
        return Response.ok().entity(TimezoneResponse.create(zoneIds, zoneIds.size())).build();
    }

    @POST
    @Path("/testInput")
    @ApiOperation(value = "Validate input credentials", response = MediaType.class)
    @NoAuditEvent("This does not change any data")
    public JsonNode checkCredentials(@ApiParam(name = "JSON body", required = true)
                                     @Valid @NotNull DBConnectorRequestImpl request) throws Exception {
        return DBConnectorDriver.checkCredentials(request);
    }

    @POST
    @Timed
    @Path("/inputs")
    @ApiOperation(value = "Create a new DB Connector input", response = MediaType.class)
    @AuditEvent(type = IntegrationsAuditEventTypes.DBCONNECTOR_INPUT_CREATE)
    @RequiresPermissions(RestPermissions.INPUTS_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull DBConnectorCreateInputRequest request) throws Exception {
        Input input = DBConnectorDriver.saveInput(request, Objects.requireNonNull(getCurrentUser()));
        return Response.ok().entity(getInputSummary(input)).build();
    }
}
