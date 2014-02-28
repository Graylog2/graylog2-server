/*
 * Copyright 2013-2014 TORCH GmbH
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.rest.resources.streams.rules;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.StreamResource;
import org.graylog2.rest.resources.streams.rules.requests.CreateRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "StreamRules", description = "Manage stream rules")
@Path("/streams/{streamid}/rules")
public class StreamRuleResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamRuleResource.class);

    @POST
    @Timed
    @ApiOperation(value = "Create a stream rule")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(title = "streamid", description = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(title = "JSON body", required = true) String body) {
        CreateRequest cr;
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        StreamImpl stream = null;
        try {
            stream = StreamImpl.load(loadObjectId(streamid), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        Map<String, Object> streamRuleData = Maps.newHashMap();
        streamRuleData.put("type", cr.type);
        streamRuleData.put("value", cr.value);
        streamRuleData.put("field", cr.field);
        streamRuleData.put("inverted", cr.inverted);
        streamRuleData.put("stream_id", new ObjectId(streamid));

        final StreamRuleImpl streamRule = new StreamRuleImpl(streamRuleData, core);

        String id;
        try {
            id = streamRule.save().toStringMongod();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("streamrule_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @POST @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Update a stream rule")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream or stream rule not found."),
            @ApiResponse(code = 400, message = "Invalid JSON Body.")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@ApiParam(title = "streamid", description = "The stream id this rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(title = "streamRuleId", description = "The stream rule id we are updating", required = true) @PathParam("streamRuleId") String streamRuleId,
                           @ApiParam(title = "JSON body", required = true) String body) {
        CreateRequest cr;
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        StreamRule streamRule;
        try {
            streamRule = StreamRuleImpl.load(loadObjectId(streamRuleId), core);
            if (!streamRule.getStreamId().equals(streamid)) {
                throw new NotFoundException();
            }
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        streamRule.setField(cr.field);
        streamRule.setType(StreamRuleType.fromInteger(cr.type));
        streamRule.setInverted(cr.inverted);
        streamRule.setValue(cr.value);

        String id;
        try {
            ((StreamRuleImpl)streamRule).save();
            id = streamRule.getId();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("streamrule_id", id);

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@ApiParam(title = "streamid", description = "The id of the stream whose stream rules we want.", required = true) @PathParam("streamid") String streamid) {
        List<Map<String, Object>> streamRules = Lists.newArrayList();
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        try {
            for (StreamRule streamRule : StreamRuleImpl.findAllForStream(streamid, core)) {
                streamRules.add(((StreamRuleImpl) streamRule).asMap());
            }
        } catch (org.graylog2.database.NotFoundException e) {
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", streamRules.size());
        result.put("stream_rules", streamRules);

        return json(result);
    }

    @GET @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Get a single stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@ApiParam(title = "streamid", description = "The id of the stream whose stream rule we want.", required = true) @PathParam("streamid") String streamid,
                      @ApiParam(title = "streamRuleId", description = "The stream rule id we are getting", required = true) @PathParam("streamRuleId") String streamRuleId) {
        StreamRule streamRule;
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        try {
            streamRule = StreamRuleImpl.load(loadObjectId(streamRuleId), core);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.OK).entity(json(((StreamRuleImpl)streamRule).asMap())).build();
    }

    @DELETE @Path("/{streamRuleId}") @Timed
    @ApiOperation(value = "Delete a stream rule")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream rule not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(title = "streamid", description = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                         @ApiParam(title = "streamRuleId", required = true) @PathParam("streamRuleId") String streamRuleId) {
        if (streamRuleId == null || streamRuleId.isEmpty()) {
            LOG.error("Missing streamRuleId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            StreamRuleImpl streamRule = StreamRuleImpl.load(loadObjectId(streamRuleId), core);
            if (streamRule.getStreamId().equals(streamid)) {
                streamRule.destroy();
            } else {
                throw new NotFoundException();
            }
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(204)).build();
    }
}
