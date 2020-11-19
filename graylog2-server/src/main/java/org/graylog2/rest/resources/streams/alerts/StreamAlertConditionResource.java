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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.AlertService;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.streams.alerts.AlertConditionListSummary;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.graylog2.rest.models.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.rest.resources.streams.responses.AlertConditionTestResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.utilities.ConfigurationMapConverter;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Stream/AlertConditions", description = "Manage stream legacy alert conditions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/streams/{streamId}/alerts/conditions")
public class StreamAlertConditionResource extends RestResource {
    private final StreamService streamService;
    private final AlertService alertService;
    private final Map<String, AlertCondition.Factory> alertConditionMap;

    @Inject
    public StreamAlertConditionResource(StreamService streamService,
                                        AlertService alertService,
                                        Map<String, AlertCondition.Factory> alertConditionMap) {
        this.streamService = streamService;
        this.alertService = alertService;
        this.alertConditionMap = alertConditionMap;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alert condition")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_CONDITION_CREATE)
    public Response create(@ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
                           @PathParam("streamId") String streamid,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateConditionRequest ccr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);

        try {
            final AlertCondition alertCondition = alertService.fromRequest(convertConfigurationInRequest(ccr), stream, getCurrentUser().getName());
            streamService.addAlertCondition(stream, alertCondition);

            final Map<String, String> result = ImmutableMap.of("alert_condition_id", alertCondition.getId());
            final URI alertConditionUri = getUriBuilderToSelf().path(StreamAlertConditionResource.class)
                    .path("{conditionId}")
                    .build(stream.getId(), alertCondition.getId());

            return Response.created(alertConditionUri).entity(result).build();
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid alert condition parameters", e);
        }
    }

    @PUT
    @Timed
    @Path("{conditionId}")
    @ApiOperation(value = "Modify an alert condition")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_CONDITION_UPDATE)
    public void update(@ApiParam(name = "streamId", value = "The stream id the alert condition belongs to.", required = true)
                       @PathParam("streamId") String streamid,
                       @ApiParam(name = "conditionId", value = "The alert condition id.", required = true)
                       @PathParam("conditionId") String conditionid,
                       @ApiParam(name = "JSON body", required = true)
                       @Valid @NotNull CreateConditionRequest ccr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);
        AlertCondition alertCondition = streamService.getAlertCondition(stream, conditionid);

        try {
            final AlertCondition updatedCondition = alertService.updateFromRequest(alertCondition, convertConfigurationInRequest(ccr));
            streamService.updateAlertCondition(stream, updatedCondition);
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid alert condition parameters", e);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all alert conditions of this stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public AlertConditionListSummary list(@ApiParam(name = "streamId", value = "The id of the stream whose alert conditions we want.", required = true)
                                          @PathParam("streamId") String streamid) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final Stream stream = streamService.load(streamid);

        final List<AlertCondition> alertConditions = streamService.getAlertConditions(stream);
        final List<AlertConditionSummary> conditionSummaries = alertConditions
                .stream()
                .map((condition) -> AlertConditionSummary.create(condition.getId(),
                        condition.getType(),
                        condition.getCreatorUserId(),
                        condition.getCreatedAt().toDate(),
                        condition.getParameters(),
                        alertService.inGracePeriod(condition),
                        condition.getTitle()))
                .collect(Collectors.toList());

        return AlertConditionListSummary.create(conditionSummaries);
    }

    @DELETE
    @Timed
    @Path("{conditionId}")
    @ApiOperation(value = "Delete an alert condition")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_CONDITION_DELETE)
    public void delete(@ApiParam(name = "streamId", value = "The stream id this alert condition belongs to.", required = true)
                       @PathParam("streamId") String streamid,
                       @ApiParam(name = "conditionId", value = "The alert condition id to be deleted", required = true)
                       @PathParam("conditionId") String conditionId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);
        streamService.removeAlertCondition(stream, conditionId);
    }

    @GET
    @Timed
    @Path("{conditionId}")
    @ApiOperation(value = "Get an alert condition")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_CONDITION_DELETE)
    public AlertConditionSummary get(@ApiParam(name = "streamId", value = "The stream id this alert condition belongs to.", required = true)
                                     @PathParam("streamId") String streamId,
                                     @ApiParam(name = "conditionId", value = "The alert condition id to be fetched", required = true)
                                     @PathParam("conditionId") String conditionId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final Stream stream = streamService.load(streamId);
        final AlertCondition condition = streamService.getAlertCondition(stream, conditionId);

        return AlertConditionSummary.create(condition.getId(),
                condition.getType(),
                condition.getCreatorUserId(),
                condition.getCreatedAt().toDate(),
                condition.getParameters(),
                alertService.inGracePeriod(condition),
                condition.getTitle());
    }


    @POST
    @Path("test")
    @Timed
    @ApiOperation("Test new alert condition")
    @NoAuditEvent("resource doesn't modify any data")
    public Response testNew(@ApiParam(name = "streamId", value = "The stream ID this alert condition belongs to.", required = true) @PathParam("streamId") String streamId,
                            @ApiParam(name = "Alert condition parameters", required = true) @Valid @NotNull CreateConditionRequest ccr) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);

        try {
            final AlertCondition alertCondition = alertService.fromRequest(convertConfigurationInRequest(ccr), stream, getCurrentUser().getName());

            return Response.ok(testAlertCondition(alertCondition)).build();
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid alert condition parameters", e);
        }
    }

    @POST
    @Path("{conditionId}/test")
    @Timed
    @ApiOperation("Test existing alert condition")
    @NoAuditEvent("resource doesn't modify any data")
    public Response testExisting(@ApiParam(name = "streamId", value = "The stream ID this alert condition belongs to.", required = true)
                                 @PathParam("streamId") String streamId,
                                 @ApiParam(name = "conditionId", value = "The alert condition ID to be fetched", required = true)
                                 @PathParam("conditionId") String conditionId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);
        final AlertCondition alertCondition = streamService.getAlertCondition(stream, conditionId);

        final AlertConditionTestResponse testResultResponse = testAlertCondition(alertCondition);

        if (testResultResponse.error()) {
            return Response.status(400).entity(testResultResponse).build();
        } else {
            return Response.ok(testResultResponse).build();
        }
    }

    private AlertConditionTestResponse testAlertCondition(AlertCondition alertCondition) {
        try {
            final AlertCondition.CheckResult checkResult = alertCondition.runCheck();

            return AlertConditionTestResponse.create(checkResult.isTriggered(), checkResult.getResultDescription());
        } catch (Exception e) {
            return AlertConditionTestResponse.createWithError(e);
        }
    }

    private CreateConditionRequest convertConfigurationInRequest(final CreateConditionRequest request) {
        final AlertCondition.Factory factory = alertConditionMap.get(request.type());
        if (factory == null) {
            throw new BadRequestException("Unable to load alert condition of type " + request.type());
        }
        final ConfigurationRequest requestedConfiguration = factory.config().getRequestedConfiguration();

        // coerce the configuration to their correct types according to the condition's requested config
        final Map<String, Object> parameters;
        try {
            parameters = ConfigurationMapConverter.convertValues(request.parameters(), requestedConfiguration);
        } catch (ValidationException e) {
            throw new BadRequestException("Invalid alert condition parameters", e);
        }

        return request.toBuilder().setParameters(parameters).build();
    }
}
