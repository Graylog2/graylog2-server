/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.rest.resources.streams;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.requests.CreateRequest;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRouter;
import org.graylog2.streams.StreamRuleImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Streams", description = "Manage streams")
@Path("/streams")
public class StreamResource extends RestResource {
	private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    @Inject
    protected Core core;

    @POST @Timed
    @ApiOperation(value = "Create a stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(title = "JSON body", required = true) String body) {
        CreateRequest cr;

        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Create stream.
        Map<String, Object> streamData = Maps.newHashMap();
        streamData.put("title", cr.title);
        streamData.put("creator_user_id", cr.creatorUserId);
        streamData.put("created_at", new DateTime(DateTimeZone.UTC));

        StreamImpl stream = new StreamImpl(streamData, core);
        ObjectId id;
        try {
            id = stream.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (cr.rules != null && cr.rules.size() > 0) {
            for (org.graylog2.rest.resources.streams.rules.requests.CreateRequest request : cr.rules) {
                Map<String, Object> streamRuleData = Maps.newHashMap();

                streamRuleData.put("type", request.type);
                streamRuleData.put("field", request.field);
                streamRuleData.put("value", request.value);
                streamRuleData.put("inverted", request.inverted);
                streamRuleData.put("stream_id", id);

                StreamRuleImpl streamRule = new StreamRuleImpl(streamRuleData, core);
                try {
                    streamRule.save();
                } catch (ValidationException e) {
                    LOG.error("Validation error while trying to save a stream rule: ", e);
                    throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
                }
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("stream_id", id.toStringMongod());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }
    
    @GET @Timed
    @ApiOperation(value = "Get a list of all streams")
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<Map<String, Object>> streams = Lists.newArrayList();
        for (Stream stream : StreamImpl.loadAll(core)) {
        	streams.add(((StreamImpl) stream).asMap());
        }
        
        Map<String, Object> result = Maps.newHashMap();
        result.put("total", streams.size());
        result.put("streams", streams);

        return json(result);
    }

    @GET @Path("/enabled") @Timed
    @ApiOperation(value = "Get a list of all streams")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEnabled() {
        List<Map<String, Object>> streams = Lists.newArrayList();
        for (Stream stream : StreamImpl.loadAllEnabled(core)) {
            streams.add(((StreamImpl) stream).asMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", streams.size());
        result.put("streams", streams);

        return json(result);
    }

    @GET @Path("/{streamId}") @Timed
    @ApiOperation(value = "Get a single stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public String get(@ApiParam(title = "streamId", required = true) @PathParam("streamId") String streamId) {
        if (streamId == null || streamId.isEmpty()) {
        	LOG.error("Missing streamId. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        StreamImpl stream;
        try {
        	stream = StreamImpl.load(loadObjectId(streamId), core);
        } catch (NotFoundException e) {
        	throw new WebApplicationException(404);
        }

        return json(stream.asMap());
    }

    @DELETE @Path("/{streamId}") @Timed
    @ApiOperation(value = "Delete a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(title = "streamId", required = true) @PathParam("streamId") String streamId) {
        if (streamId == null || streamId.isEmpty()) {
        	LOG.error("Missing streamId. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        try {
        	StreamImpl stream = StreamImpl.load(loadObjectId(streamId), core);
        	stream.destroy();
        } catch (NotFoundException e) {
        	throw new WebApplicationException(404);
        }
        
        return Response.status(Response.Status.fromStatusCode(204)).build();
    }

    @POST @Path("/{streamId}/pause") @Timed
    @ApiOperation(value = "Pause a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public Response pause(@ApiParam(title = "streamId", required = true) @PathParam("streamId") String streamId) {
        if (streamId == null || streamId.isEmpty()) {
            LOG.error("Missing streamId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            StreamImpl stream = StreamImpl.load(loadObjectId(streamId), core);
            stream.pause();
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(200)).build();
    }

    @POST @Path("/{streamId}/resume") @Timed
    @ApiOperation(value = "Resume a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public Response resume(@ApiParam(title = "streamId", required = true) @PathParam("streamId") String streamId) {
        if (streamId == null || streamId.isEmpty()) {
            LOG.error("Missing streamId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            StreamImpl stream = StreamImpl.load(loadObjectId(streamId), core);
            stream.resume();
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(200)).build();
    }

    @POST @Path("/{streamId}/testMatch") @Timed
    @ApiOperation(value = "Test matching of a stream against a supplied message")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public String testMatch(@ApiParam(title = "streamId", required = true) @PathParam("streamId") String streamId, @ApiParam(title = "JSON body", required = true) String body) {
        if (body == null || body.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        Map<String, Map<String, Object>> serialisedMessage;
        try {
            serialisedMessage = objectMapper.readValue(body, new TypeReference<Map<String, Map<String, Object>>>() {});
        } catch (IOException e) {
            LOG.error("Received invalid JSON body. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (serialisedMessage.get("message") == null) {
            LOG.error("Received invalid JSON body. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        StreamImpl stream = fetchStream(streamId);
        Message message = new Message(serialisedMessage.get("message"));

        StreamRouter router = new StreamRouter(false);

        Map<StreamRule, Boolean> ruleMatches = router.getRuleMatches(core, stream, message);
        Map<String, Boolean> rules = Maps.newHashMap();

        for (StreamRule ruleMatch : ruleMatches.keySet()) {
            rules.put(ruleMatch.getObjectId().toStringMongod(), ruleMatches.get(ruleMatch));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("matches", router.doesStreamMatch(ruleMatches));
        result.put("rules", rules);

        return json(result);
    }

    protected StreamImpl fetchStream(String streamId) {
        if (streamId == null || streamId.isEmpty()) {
            LOG.error("Missing streamId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            StreamImpl stream = StreamImpl.load(loadObjectId(streamId), core);
            return stream;
        } catch (NotFoundException e) {
            throw new WebApplicationException(404);
        }
    }
}
