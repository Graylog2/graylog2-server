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
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationImpl;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackListSummary;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSummary;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbacksResponse;
import org.graylog2.rest.models.alarmcallbacks.responses.CreateAlarmCallbackResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.utilities.ConfigurationMapConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
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

@RequiresAuthentication
@Api(value = "Stream/AlarmCallbacks", description = "Manage stream legacy alarm callbacks")
@Path("/streams/{streamid}/alarmcallbacks")
public class StreamAlarmCallbackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamAlarmCallbackResource.class);

    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final StreamService streamService;
    private final AlarmCallbackFactory alarmCallbackFactory;
    private final AlarmCallbacksResource alarmCallbacksResource;

    @Inject
    public StreamAlarmCallbackResource(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                       StreamService streamService,
                                       AlarmCallbackFactory alarmCallbackFactory,
                                       AlarmCallbacksResource alarmCallbacksResource) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.streamService = streamService;
        this.alarmCallbackFactory = alarmCallbackFactory;
        this.alarmCallbacksResource = alarmCallbacksResource;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public AlarmCallbackListSummary get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                        @PathParam("streamid") String streamid) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        final Stream stream = streamService.load(streamid);

        final List<AlarmCallbackSummary> alarmCallbacks = Lists.newArrayList();
        for (AlarmCallbackConfiguration callback : alarmCallbackConfigurationService.getForStream(stream)) {
            alarmCallbacks.add(AlarmCallbackSummary.create(
                    callback.getId(),
                    callback.getStreamId(),
                    callback.getType(),
                    callback.getTitle(),
                    callback.getConfiguration(),
                    callback.getCreatedAt(),
                    callback.getCreatorUserId()
            ));
        }

        return AlarmCallbackListSummary.create(alarmCallbacks);
    }

    @GET
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Get a single specified alarm callback for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public AlarmCallbackSummary get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                    @PathParam("streamid") String streamid,
                                    @ApiParam(name = "alarmCallbackId", value = "The alarm callback id we are getting", required = true)
                                    @PathParam("alarmCallbackId") String alarmCallbackId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        final Stream stream = streamService.load(streamid);

        final AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId())) {
            throw new javax.ws.rs.NotFoundException("Couldn't find alarm callback " + alarmCallbackId + " in for steam " + streamid);
        }

        return AlarmCallbackSummary.create(result.getId(), result.getStreamId(), result.getType(), result.getTitle(), result.getConfiguration(), result.getCreatedAt(), result.getCreatorUserId());
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alarm callback",
            response = CreateAlarmCallbackResponse.class)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ALARM_CALLBACK_CREATE)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new alarm callback belongs to.", required = true)
                           @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true) CreateAlarmCallbackRequest originalCr) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        // make sure the values are correctly converted to the declared configuration types
        final CreateAlarmCallbackRequest cr = CreateAlarmCallbackRequest.create(originalCr.type(), originalCr.title(), convertConfigurationValues(originalCr));

        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackConfigurationService.create(streamid, cr, getCurrentUser().getName());

        final String id;
        try {
            alarmCallbackFactory.create(alarmCallbackConfiguration).checkConfiguration();
            id = alarmCallbackConfigurationService.save(alarmCallbackConfiguration);
        } catch (ValidationException | AlarmCallbackConfigurationException | ConfigurationException e) {
            LOG.error("Invalid alarm callback configuration.", e);
            throw new BadRequestException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            LOG.error("Invalid alarm callback type.", e);
            throw new BadRequestException("Invalid alarm callback type.", e);
        }

        final URI alarmCallbackUri = getUriBuilderToSelf().path(StreamAlarmCallbackResource.class)
                .path("{alarmCallbackId}")
                .build(streamid, id);

        return Response.created(alarmCallbackUri).entity(CreateAlarmCallbackResponse.create(id)).build();
    }

    @GET
    @Path("/available")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callback types")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public AvailableAlarmCallbacksResponse available(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                                     @PathParam("streamid") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        return alarmCallbacksResource.available();
    }

    @DELETE
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Delete an alarm callback")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Alarm callback not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(type = AuditEventTypes.ALARM_CALLBACK_DELETE)
    public void delete(@ApiParam(name = "streamid", value = "The stream id this alarm callback belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "alarmCallbackId", required = true)
                       @PathParam("alarmCallbackId") String alarmCallbackId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);
        final Stream stream = streamService.load(streamid);

        final AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId())) {
            throw new javax.ws.rs.NotFoundException("Couldn't find alarm callback " + alarmCallbackId + " in for steam " + streamid);
        }

        if (alarmCallbackConfigurationService.destroy(result) == 0) {
            final String msg = "Couldn't remove alarm callback with ID " + result.getId();
            LOG.error(msg);
            throw new InternalServerErrorException(msg);
        }
    }

    @PUT
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Update an alarm callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.ALARM_CALLBACK_UPDATE)
    public void update(@ApiParam(name = "streamid", value = "The stream id this alarm callback belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "alarmCallbackId", required = true)
                       @PathParam("alarmCallbackId") String alarmCallbackId,
                       @ApiParam(name = "JSON body", required = true) CreateAlarmCallbackRequest alarmCallbackRequest) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final AlarmCallbackConfiguration callbackConfiguration = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (callbackConfiguration == null) {
            throw new NotFoundException("Unable to find alarm callback configuration " + alarmCallbackId);
        }

        final Map<String, Object> configuration = convertConfigurationValues(alarmCallbackRequest);

        final AlarmCallbackConfiguration updatedConfig = ((AlarmCallbackConfigurationImpl) callbackConfiguration).toBuilder()
                .setTitle(alarmCallbackRequest.title())
                .setConfiguration(configuration)
                .build();

        try {
            alarmCallbackFactory.create(updatedConfig).checkConfiguration();
            alarmCallbackConfigurationService.save(updatedConfig);
        } catch (ValidationException | AlarmCallbackConfigurationException | ConfigurationException e) {
            LOG.error("Invalid alarm callback configuration.", e);
            throw new BadRequestException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            LOG.error("Invalid alarm callback type.", e);
            throw new BadRequestException("Invalid alarm callback type.", e);
        }
    }

    private Map<String, Object> convertConfigurationValues(final CreateAlarmCallbackRequest alarmCallbackRequest) {
        final ConfigurationRequest requestedConfiguration;
        try {
            final AlarmCallback alarmCallback = alarmCallbackFactory.create(alarmCallbackRequest.type());
            requestedConfiguration = alarmCallback.getRequestedConfiguration();
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("Unable to load alarm callback of type " + alarmCallbackRequest.type(), e);
        }

        // coerce the configuration to their correct types according to the alarmcallback's requested config
        final Map<String, Object> configuration;
        try {
            configuration = ConfigurationMapConverter.convertValues(alarmCallbackRequest.configuration(),
                                                                    requestedConfiguration);
        } catch (ValidationException e) {
            throw new BadRequestException("Invalid configuration map", e);
        }
        return configuration;
    }
}
