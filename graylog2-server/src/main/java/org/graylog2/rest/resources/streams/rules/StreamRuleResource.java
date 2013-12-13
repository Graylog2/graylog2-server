package org.graylog2.rest.resources.streams.rules;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.*;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.StreamResource;
import org.graylog2.rest.resources.streams.rules.requests.CreateRequest;
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
@Api(value = "StreamRules", description = "Manage stream rules")
@Path("/streams/{streamid}/rules")
public class StreamRuleResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    @POST
    @Timed
    @ApiOperation(value = "Create a stream rule")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(title = "streamid", description = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(title = "JSON body", required = true) String body) {
        CreateRequest cr;

        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        final List<Stream> streams = StreamImpl.loadAll(core);

        StreamImpl stream = null;

        for (Stream s : streams) {
            if (s.getId().toString().equals(streamid)) {
                stream = (StreamImpl) s;
                break;
            }
        }

        if (stream == null) {
            throw new WebApplicationException("Stream " + streamid + " not found");
        }

        Map<String, Object> streamRuleData = Maps.newHashMap();
        streamRuleData.put("type", cr.type);
        streamRuleData.put("value", cr.value);
        streamRuleData.put("field", cr.field);
        streamRuleData.put("inverted", cr.inverted);
        streamRuleData.put("stream_id", new ObjectId(streamid));

        final StreamRuleImpl streamRule = new StreamRuleImpl(streamRuleData, core);

        ObjectId id;
        try {
            id = streamRule.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("streamrule_id", id.toStringMongod());

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

        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        StreamRule streamRule;
        try {
            streamRule = StreamRuleImpl.load(loadObjectId(streamRuleId), core);
            if (streamRule.getStreamId().toString().equals(streamid)) {
            } else {
                throw new NotFoundException();
            }
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        streamRule.setField(cr.field);
        streamRule.setType(StreamRuleType.fromInteger(cr.type));
        streamRule.setInverted(cr.inverted);
        streamRule.setValue(cr.value);

        ObjectId id;
        try {
            id = ((StreamRuleImpl)streamRule).save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("streamrule_id", id.toStringMongod());

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@ApiParam(title = "streamid", description = "The id of the stream whose stream rules we want.", required = true) @PathParam("streamid") String streamid) {
        List<Map<String, Object>> streamRules = Lists.newArrayList();

        try {
            for (StreamRule streamRule : StreamRuleImpl.findAllForStream(new ObjectId(streamid), core)) {
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

        try {
            StreamRuleImpl streamRule = StreamRuleImpl.load(loadObjectId(streamRuleId), core);
            if (streamRule.getStreamId().toString().equals(streamid)) {
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
