/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.alarmcallbacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.CreateAlarmCallbackRequest;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

    @Inject
    public AlarmCallbackResource(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                 StreamService streamService,
                                 Set<AlarmCallback> availableAlarmCallbacks) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.streamService = streamService;
        this.availableAlarmCallbacks = availableAlarmCallbacks;
    }

    // TODO: add permission checks

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                   @PathParam("streamid") String streamid) throws NotFoundException {
        final Stream stream = streamService.load(streamid);

        final List<Map<String, Object>> alarmCallbacks = Lists.newArrayList();
        for (AlarmCallbackConfiguration callback : alarmCallbackConfigurationService.getForStream(stream)) {
            alarmCallbacks.add(callback.getFields());
        }

        return ImmutableMap.of(
                "alarmcallbacks", alarmCallbacks,
                "total", alarmCallbacks.size());
    }

    @GET
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Get a single specified alarm callback for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                   @PathParam("streamid") String streamid,
                                   @ApiParam(name = "alarmCallbackId", value = "The alarm callback id we are getting", required = true)
                                   @PathParam("alarmCallbackId") String alarmCallbackId) throws NotFoundException {
        final Stream stream = streamService.load(streamid);

        final AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId())) {
            throw new javax.ws.rs.NotFoundException();
        }

        return result.getFields();
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alarm callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new alarm callback belongs to.", required = true)
                           @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true) CreateAlarmCallbackRequest cr) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);

        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackConfigurationService.create(streamid, cr, getCurrentUser().getName());
        alarmCallbackConfiguration.setStream(stream);

        final String id;
        try {
            id = alarmCallbackConfigurationService.save(alarmCallbackConfiguration);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException(e);
        }

        final Map<String, String> result = ImmutableMap.of("alarmcallback_id", id);
        final URI alarmCallbackUri = UriBuilder.fromResource(AlarmCallbackResource.class)
                .path("{alarmCallbackId}")
                .build(id);

        return Response.created(alarmCallbackUri).entity(result).build();
    }

    @GET
    @Path("/available")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callback types")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Object>> available(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true)
                                                      @PathParam("streamid") String streamid) {
        final Map<String, Object> types = Maps.newHashMapWithExpectedSize(availableAlarmCallbacks.size());
        for (AlarmCallback availableAlarmCallback : availableAlarmCallbacks) {
            Map<String, Object> type = Maps.newHashMap();
            type.put("requested_configuration", availableAlarmCallback.getRequestedConfiguration().asList());
            type.put("name", availableAlarmCallback.getName());
            types.put(availableAlarmCallback.getClass().getCanonicalName(), type);
        }

        return ImmutableMap.of("types", types);
    }

    @DELETE
    @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Delete an alarm callback")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Alarm callback not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public void delete(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "alarmCallbackId", required = true)
                       @PathParam("alarmCallbackId") String alarmCallbackId) throws NotFoundException {
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
}