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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.CreateAlarmCallbackRequest;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.streams.Stream;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
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
    public Response get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true) @PathParam("streamid") String streamid) {
        Stream stream = null;
        try {
            stream = streamService.load(streamid);
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }

        Map<String, Object> result = Maps.newHashMap();
        List<Map<String, Object>> alarmCallbacks = Lists.newArrayList();
        for (AlarmCallbackConfiguration callback : alarmCallbackConfigurationService.getForStream(stream)) {
            alarmCallbacks.add(callback.getFields());
        }

        result.put("alarmcallbacks", alarmCallbacks);
        result.put("total", alarmCallbacks.size());
        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @GET @Path("/{alarmCallbackId}")
    @Timed
    @ApiOperation(value = "Get a single specified alarm callback for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true) @PathParam("streamid") String streamid,
                        @ApiParam(name = "alarmCallbackId", value = "The alarm callback id we are getting", required = true) @PathParam("alarmCallbackId") String alarmCallbackId) {
        Stream stream = null;
        try {
            stream = streamService.load(streamid);
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }

        AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId()))
            throw new WebApplicationException(404);
        return Response.status(Response.Status.OK).entity(json(result.getFields())).build();
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alarm callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new alarm callback belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true) String body) {
        CreateAlarmCallbackRequest cr;
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            cr = objectMapper.readValue(body, CreateAlarmCallbackRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackConfigurationService.create(streamid, cr, getCurrentUser().getName());
        alarmCallbackConfiguration.setStream(stream);

        String id;
        try {
            id = alarmCallbackConfigurationService.save(alarmCallbackConfiguration);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("alarmcallback_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Path("/available")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callback types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response available(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks we want.", required = true) @PathParam("streamid") String streamid) {
        Map<String, Object> result = Maps.newHashMap();
        Map<String, Object> types = Maps.newHashMap();

        for (AlarmCallback availableAlarmCallback : availableAlarmCallbacks) {
            Map<String, Object> type = Maps.newHashMap();
            type.put("requested_configuration", availableAlarmCallback.getRequestedConfiguration().asList());
            type.put("name", availableAlarmCallback.getName());
            types.put(availableAlarmCallback.getClass().getCanonicalName(), type);
        }

        result.put("types", types);

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @DELETE @Path("/{alarmCallbackId}") @Timed
    @ApiOperation(value = "Delete an alarm callback")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Alarm callback not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(name = "alarmCallbackId", required = true) @PathParam("alarmCallbackId") String alarmCallbackId) {

        Stream stream = null;
        try {
            stream = streamService.load(streamid);
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }

        AlarmCallbackConfiguration result = alarmCallbackConfigurationService.load(alarmCallbackId);
        if (result == null || !result.getStreamId().equals(stream.getId()))
            throw new WebApplicationException(404);

        if (alarmCallbackConfigurationService.destroy(result) > 0)
            return Response.status(Response.Status.NO_CONTENT).build();
        else
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
