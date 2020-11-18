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
package org.graylog2.rest.resources.alarmcallbacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackListSummary;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSummary;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbackSummaryResponse;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbacksResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;
import static org.graylog2.shared.security.RestPermissions.USERS_LIST;

@RequiresAuthentication
@Api(value = "AlarmCallbacks", description = "Manage legacy alarm callbacks (aka alert notifications)")
@Path("/alerts/callbacks")
@Produces(MediaType.APPLICATION_JSON)
public class AlarmCallbacksResource extends RestResource {
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final StreamService streamService;
    private final Set<AlarmCallback> availableAlarmCallbacks;
    private final AlarmCallbackFactory alarmCallbackFactory;

    @Inject
    public AlarmCallbacksResource(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                  StreamService streamService,
                                  Set<AlarmCallback> availableAlarmCallbacks,
                                  AlarmCallbackFactory alarmCallbackFactory) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.streamService = streamService;
        this.availableAlarmCallbacks = availableAlarmCallbacks;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks")
    public AlarmCallbackListSummary all() throws NotFoundException {
        final List<AlarmCallbackSummary> alarmCallbacks = streamService.loadAll().stream()
                .filter(stream -> isPermitted(STREAMS_READ, stream.getId()))
                .flatMap(stream -> alarmCallbackConfigurationService.getForStream(stream).stream()
                        .map(callback -> AlarmCallbackSummary.create(
                                callback.getId(),
                                callback.getStreamId(),
                                callback.getType(),
                                callback.getTitle(),
                                callback.getConfiguration(),
                                callback.getCreatedAt(),
                                callback.getCreatorUserId()
                        )))
                .collect(Collectors.toList());

        return AlarmCallbackListSummary.create(alarmCallbacks);
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks types")
    public AvailableAlarmCallbacksResponse available() {
        final Map<String, AvailableAlarmCallbackSummaryResponse> types = Maps.newHashMapWithExpectedSize(availableAlarmCallbacks.size());
        for (AlarmCallback availableAlarmCallback : availableAlarmCallbacks) {
            final AvailableAlarmCallbackSummaryResponse type = new AvailableAlarmCallbackSummaryResponse();
            type.name = availableAlarmCallback.getName();
            type.requested_configuration = getConfigurationRequest(availableAlarmCallback).asList();
            types.put(availableAlarmCallback.getClass().getCanonicalName(), type);
        }

        final AvailableAlarmCallbacksResponse response = new AvailableAlarmCallbacksResponse();
        response.types = types;

        return response;
    }

    @POST
    @Timed
    @Path("/{alarmCallbackId}/test")
    @ApiOperation(value = "Send a test alert for a given alarm callback")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Alarm callback not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId."),
            @ApiResponse(code = 500, message = "Error while testing alarm callback")
    })
    @NoAuditEvent("only used to test alert notifications")
    public Response test(@ApiParam(name = "alarmCallbackId", value = "The alarm callback id to send a test alert for.", required = true)
                         @PathParam("alarmCallbackId") String alarmCallbackId) throws TransportConfigurationException, EmailException, NotFoundException {
        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackConfigurationService.load(alarmCallbackId);
        final String streamId = alarmCallbackConfiguration.getStreamId();
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);

        final DummyAlertCondition testAlertCondition = new DummyAlertCondition(stream, null, Tools.nowUTC(), getSubject().getPrincipal().toString(), Collections.emptyMap(), "Test Alert");
        try {
            AbstractAlertCondition.CheckResult checkResult = testAlertCondition.runCheck();
            AlarmCallback alarmCallback = alarmCallbackFactory.create(alarmCallbackConfiguration);
            alarmCallback.call(stream, checkResult);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        return Response.ok().build();
    }

    /* This is used to add user auto-completion to EmailAlarmCallback when the current user has permissions to list users */
    private ConfigurationRequest getConfigurationRequest(AlarmCallback callback) {
        if (callback instanceof EmailAlarmCallback && isPermitted(USERS_LIST)) {
            return ((EmailAlarmCallback) callback).getEnrichedRequestedConfiguration();
        }

        return callback.getRequestedConfiguration();
    }
}
