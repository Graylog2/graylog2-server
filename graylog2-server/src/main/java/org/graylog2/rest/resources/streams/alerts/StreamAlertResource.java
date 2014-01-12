/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.database.*;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Alerts", description = "Manage stream alerts")
@Path("/streams/{streamId}/alerts")
public class StreamAlertResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertResource.class);

    @POST @Timed
    @Path("conditions")
    @ApiOperation(value = "Create a alert condition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response create(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
                           @ApiParam(title = "JSON body", required = true) String body) {
        CreateConditionRequest ccr;
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            ccr = objectMapper.readValue(body, CreateConditionRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        final AlertCondition alertCondition;
        try {
            alertCondition = AlertCondition.fromRequest(ccr, stream, core);
        } catch (AlertCondition.NoSuchAlertConditionTypeException e) {
            LOG.error("Invalid alarm condition type.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        try {
            stream.addAlertCondition(alertCondition);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("alert_condition_id", alertCondition.getId());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get the " + Alert.MAX_LIST_COUNT + " most recent alarms of this stream.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response list(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        List<Map<String,Object>> conditions = Lists.newArrayList();
        for(Alert alert : Alert.loadRecentOfStream(core, stream.getId())) {
            conditions.add(alert.toMap());
        }

        long total = Alert.totalCount(core, Alert.COLLECTION);

        Map<String, Object> result = Maps.newHashMap();
        result.put("alerts", conditions);
        result.put("total", total);

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @GET @Timed
    @Path("conditions")
    @ApiOperation(value = "Get all alert conditions of this stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response listConditions(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        List<Map<String, Object>> conditions = Lists.newArrayList();
        for (AlertCondition alertCondition : stream.getAlertConditions()) {
            conditions.add(alertCondition.asMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("conditions", conditions);
        result.put("total", conditions.size());

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @DELETE @Timed
    @Path("conditions/{conditionId}")
    @ApiOperation(value = "Delete an alert condition")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response list(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
                         @ApiParam(title = "conditionId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("conditionId") String conditionId) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        stream.removeAlertCondition(conditionId);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST @Timed
    @Path("receivers")
    @ApiOperation(value = "Add an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response addReceiver(
            @ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
            @ApiParam(title = "entity", description = "Name/ID of user or email address to add as alert receiver.", required = true) @QueryParam("entity") String entity,
            @ApiParam(title = "type", description = "Type: users or emails", required = true) @QueryParam("type") String type
            ) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        if(!type.equals("users") && !type.equals("emails")) {
            LOG.warn("No such type: [{}]", type);
            throw new WebApplicationException(400);
        }

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        // Maybe the list already contains this receiver?
        if (stream.getAlertReceivers().containsKey(type) || stream.getAlertReceivers().get(type) != null) {
            if (stream.getAlertReceivers().get(type).contains(entity)) {
                return Response.status(Response.Status.CREATED).build();
            }
        }

        stream.addAlertReceiver(type, entity);

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE @Timed
    @Path("receivers")
    @ApiOperation(value = "Remove an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response removeReceiver(
            @ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
            @ApiParam(title = "entity", description = "Name/ID of user or email address to remove from alert receivers.", required = true) @QueryParam("entity") String entity,
            @ApiParam(title = "type", description = "Type: users or emails", required = true) @QueryParam("type") String type) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        if(!type.equals("users") && !type.equals("emails")) {
            LOG.warn("No such type: [{}]", type);
            throw new WebApplicationException(400);
        }

        StreamImpl stream;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        stream.removeAlertReceiver(type, entity);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
