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
import org.graylog2.alerts.AlertCondition;
import org.graylog2.database.*;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamImpl;
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
@Path("/streams/{streamid}/alerts")
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
    public Response create(@ApiParam(title = "streamid", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamid") String streamid,
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

        final AlertCondition alertCondition = new AlertCondition(ccr, Tools.iso8601());

        try {
            stream.embed(StreamImpl.EMBEDDED_ALERT_CONDITIONS, alertCondition);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("alert_condition_id", alertCondition.getId());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @Path("conditions")
    @ApiOperation(value = "Get all alert conditions of this stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response create(@ApiParam(title = "streamid", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamid") String streamid) {
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

}
