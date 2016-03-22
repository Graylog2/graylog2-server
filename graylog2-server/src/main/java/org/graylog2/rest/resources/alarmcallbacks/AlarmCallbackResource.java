/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.alarmcallbacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationAVImpl;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackListSummary;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSummary;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbackSummaryResponse;
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
import java.util.Set;

@RequiresAuthentication
@Api(value = "AlarmCallbacks", description = "Manage stream alarm callbacks")
@Path("/streams/{streamid}/alarmcallbacks")
public class AlarmCallbackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmCallbackResource.class);

    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final StreamService streamService;
    private final Set<AlarmCallback> availableAlarmCallbacks;
    private final AlarmCallbackFactory alarmCallbackFactory;

    @Inject
    public AlarmCallbackResource(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                 StreamService streamService,
                                 Set<AlarmCallback> availableAlarmCallbacks,
                                 AlarmCallbackFactory alarmCallbackFactory) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.streamService = streamService;
        this.availableAlarmCallbacks = availableAlarmCallbacks;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    // TODO: add permission checks

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
            throw new javax.ws.rs.NotFoundException();
        }

        return AlarmCallbackSummary.create(result.getId(), result.getStreamId(), result.getType(), result.getConfiguration(), result.getCreatedAt(), result.getCreatorUserId());
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alarm callback",
            response = CreateAlarmCallbackResponse.class)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new alarm callback belongs to.", required = true)
                           @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true) CreateAlarmCallbackRequest cr) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        // make sure the values are correctly converted to the declared configuration types
        cr.configuration = convertConfigurationValues(cr);

        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackConfigurationService.create(streamid, cr, getCurrentUser().getName());

        final String id;
        try {
            id = alarmCallbackConfigurationService.save(alarmCallbackConfiguration);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException(e);
        }

        final URI alarmCallbackUri = getUriBuilderToSelf().path(AlarmCallbackResource.class)
                .path("{alarmCallbackId}")
                .build(streamid, id);

        return Response.created(alarmCallbackUri).entity(CreateAlarmCallbackResponse.create(id)).build();
    }

    @GET
    @Path("/available")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callback types")
    @Produces(MediaType.APPLICATION_JSON)
    public AvailableAlarmCallbacksResponse available(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                                      @PathParam("streamid") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);
        final Map<String, AvailableAlarmCallbackSummaryResponse> types = Maps.newHashMapWithExpectedSize(availableAlarmCallbacks.size());
        for (AlarmCallback availableAlarmCallback : availableAlarmCallbacks) {
            final AvailableAlarmCallbackSummaryResponse type = new AvailableAlarmCallbackSummaryResponse();
            type.name = availableAlarmCallback.getName();
            type.requested_configuration = availableAlarmCallback.getRequestedConfiguration().asList();
            types.put(availableAlarmCallback.getClass().getCanonicalName(), type);
        }

        final AvailableAlarmCallbacksResponse response = new AvailableAlarmCallbacksResponse();
        response.types = types;

        return response;
    }

    @DELETE
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Delete an alarm callback")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Alarm callback not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public void delete(@ApiParam(name = "streamid", value = "The stream id this alarm callback belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "alarmCallbackId", required = true)
                       @PathParam("alarmCallbackId") String alarmCallbackId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);
        final Stream stream = streamService.load(streamid);

        final AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId())) {
            throw new javax.ws.rs.NotFoundException();
        }

        if (alarmCallbackConfigurationService.destroy(result) == 0) {
            LOG.error("Couldn't remove alarm callback with id {}", result.getId());
            throw new InternalServerErrorException();
        }
    }

    @PUT
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Update an alarm callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void update(@ApiParam(name = "streamid", value = "The stream id this alarm callback belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "alarmCallbackId", required = true)
                       @PathParam("alarmCallbackId") String alarmCallbackId,
                       @ApiParam(name = "JSON body", required = true) CreateAlarmCallbackRequest alarmCallbackRequest) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final AlarmCallbackConfiguration aCC = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (aCC == null) {
            throw new NotFoundException("Unable to find alarm callback configuration " + alarmCallbackId);
        }

        final Map<String, Object> configuration = convertConfigurationValues(alarmCallbackRequest);

        final AlarmCallbackConfigurationAVImpl newConfig = AlarmCallbackConfigurationAVImpl.create(
                alarmCallbackId,
                aCC.getStreamId(),
                aCC.getType(),
                configuration,
                aCC.getCreatedAt(),
                aCC.getCreatorUserId());

        try {
             alarmCallbackConfigurationService.save(newConfig);
        } catch (ValidationException e) {
            throw new BadRequestException("Unable to save alarm callback configuration", e);
        }
    }

    private Map<String, Object> convertConfigurationValues(final CreateAlarmCallbackRequest alarmCallbackRequest) {
        final ConfigurationRequest requestedConfiguration;
        try {
            final AlarmCallback alarmCallback = alarmCallbackFactory.create(alarmCallbackRequest.type);
            requestedConfiguration = alarmCallback.getRequestedConfiguration();
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("Unable to load alarm callback of type " + alarmCallbackRequest.type, e);
        }

        // coerce the configuration to their correct types according to the alarmcallback's requested config
        final Map<String, Object> configuration;
        try {
            configuration = ConfigurationMapConverter.convertValues(alarmCallbackRequest.configuration,
                                                                    requestedConfiguration);
        } catch (ValidationException e) {
            throw new BadRequestException("Invalid configuration map", e);
        }
        return configuration;
    }
}
