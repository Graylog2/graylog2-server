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
package org.graylog2.rest.resources.streams;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.cliffc.high_scale_lib.Counter;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.requests.CreateRequest;
import org.graylog2.rest.resources.streams.responses.StreamListResponse;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.stats.ThroughputStats;
import org.graylog2.streams.StreamRouter;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Streams", description = "Manage streams")
@Path("/streams")
public class StreamResource extends RestResource {
	private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final ThroughputStats throughputStats;
    private final StreamRouter streamRouter;

    @Inject
    public StreamResource(StreamService streamService,
                          StreamRuleService streamRuleService,
                          ThroughputStats throughputStats,
                          StreamRouter streamRouter) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.throughputStats = throughputStats;
        this.streamRouter = streamRouter;
    }

    @POST @Timed
    @ApiOperation(value = "Create a stream")
    @RequiresPermissions(RestPermissions.STREAMS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "JSON body", required = true) final CreateRequest cr) throws ValidationException {
        checkPermission(RestPermissions.STREAMS_CREATE);

        // Create stream.

        final Stream stream = streamService.create(cr, getCurrentUser().getName());
        stream.setDisabled(true);

        final String id = streamService.save(stream);

        if (cr.rules != null && cr.rules.size() > 0) {
            for (CreateStreamRuleRequest request : cr.rules) {
                StreamRule streamRule = streamRuleService.create(id, request);
                streamRuleService.save(streamRule);
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("stream_id", id);

        return Response.status(Response.Status.CREATED).entity(result).build();
    }
    
    @GET @Timed
    @ApiOperation(value = "Get a list of all streams")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamListResponse get() {
        StreamListResponse response = new StreamListResponse();
        response.streams = new ArrayList<>();

        for (Stream stream : streamService.loadAll())
            if (isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                response.streams.add(stream);

        response.total = response.streams.size();

        return response;
    }

    @GET @Path("/enabled") @Timed
    @ApiOperation(value = "Get a list of all enabled streams")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamListResponse getEnabled() throws NotFoundException {
        StreamListResponse response = new StreamListResponse();
        response.streams = new ArrayList<>();
        for (Stream stream : streamService.loadAllEnabled())
            if (isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                response.streams.add(stream);
        response.total = response.streams.size();

        return response;
    }

    @GET @Path("/{streamId}") @Timed
    @ApiOperation(value = "Get a single stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Stream get(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException {
        if (streamId == null || streamId.isEmpty()) {
        	LOG.error("Missing streamId. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        return streamService.load(streamId);
    }

    @PUT @Timed
    @Path("/{streamId}")
    @ApiOperation(value = "Update a stream")
    @RequiresPermissions(RestPermissions.STREAMS_EDIT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Stream update(@ApiParam(name = "JSON body", required = true) CreateRequest cr, @ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        final Stream stream = streamService.load(streamId);

        stream.setTitle(cr.title);
        stream.setDescription(cr.description);

        streamService.save(stream);

        return stream;
    }

    @DELETE @Path("/{streamId}") @Timed
    @ApiOperation(value = "Delete a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        Stream stream = streamService.load(streamId);
        streamService.destroy(stream);

        return Response.noContent().build();
    }

    @POST @Path("/{streamId}/pause") @Timed
    @ApiOperation(value = "Pause a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public Response pause(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException, ValidationException {
        if (streamId == null || streamId.isEmpty()) {
            LOG.error("Missing streamId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.STREAMS_CHANGESTATE, streamId);

        final Stream stream = streamService.load(streamId);
        streamService.pause(stream);

        return Response.ok().build();
    }

    @POST @Path("/{streamId}/resume") @Timed
    @ApiOperation(value = "Resume a stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public Response resume(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException, ValidationException {
        if (streamId == null || streamId.isEmpty()) {
            LOG.error("Missing streamId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.STREAMS_CHANGESTATE, streamId);

        final Stream stream = streamService.load(streamId);
        streamService.resume(stream);

        return Response.ok().build();
    }

    @POST @Path("/{streamId}/testMatch") @Timed
    @ApiOperation(value = "Test matching of a stream against a supplied message")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public String testMatch(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId, @ApiParam(name = "JSON body", required = true) Map<String, Map<String, Object>> serialisedMessage) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        if (serialisedMessage.get("message") == null) {
            LOG.error("Received invalid JSON body. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        final Stream stream = streamService.load(streamId);
        final Message message = new Message(serialisedMessage.get("message"));

        final Map<StreamRule, Boolean> ruleMatches = streamRouter.getRuleMatches(stream, message);
        final Map<String, Boolean> rules = Maps.newHashMap();

        for (StreamRule ruleMatch : ruleMatches.keySet())
            rules.put(ruleMatch.getId(), ruleMatches.get(ruleMatch));

        final Map<String, Object> result = Maps.newHashMap();
        result.put("matches", streamRouter.doesStreamMatch(ruleMatches));
        result.put("rules", rules);

        return json(result);
    }

    @POST @Path("/{streamId}/clone") @Timed
    @ApiOperation(value = "Clone a stream")
    @RequiresPermissions(RestPermissions.STREAMS_CREATE)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cloneStream(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId,
                          @ApiParam(name = "JSON body", required = true) CreateRequest cr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_CREATE);
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final Stream sourceStream = streamService.load(streamId);

        // Create stream.
        final Map<String, Object> streamData = Maps.newHashMap();
        streamData.put("title", cr.title);
        streamData.put("description", cr.description);
        streamData.put("creator_user_id", getCurrentUser().getName());
        streamData.put("created_at", Tools.iso8601());

        final Stream stream = streamService.create(streamData);
        streamService.pause(stream);
        final String id;
        streamService.save(stream);
        id = stream.getId();

        final List<StreamRule> sourceStreamRules;

        sourceStreamRules = streamRuleService.loadForStream(sourceStream);

        if (sourceStreamRules.size() > 0) {
            for (StreamRule streamRule : sourceStreamRules) {
                Map<String, Object> streamRuleData = Maps.newHashMap();

                streamRuleData.put("type", streamRule.getType().toInteger());
                streamRuleData.put("field", streamRule.getField());
                streamRuleData.put("value", streamRule.getValue());
                streamRuleData.put("inverted", streamRule.getInverted());
                streamRuleData.put("stream_id", new ObjectId(id));

                StreamRule newStreamRule = streamRuleService.create(streamRuleData);
                streamRuleService.save(newStreamRule);
            }
        }

        for (AlertCondition alertCondition : streamService.getAlertConditions(sourceStream))
                streamService.addAlertCondition(stream, alertCondition);

        for (Map.Entry<String, List<String>> entry : sourceStream.getAlertReceivers().entrySet())
            for (String receiver : entry.getValue())
                streamService.addAlertReceiver(stream, entry.getKey(), receiver);

        for (Output output : sourceStream.getOutputs())
            streamService.addOutput(stream, output);

        Map<String, Object> result = Maps.newHashMap();
        result.put("stream_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @Path("/{streamId}/throughput")
    @ApiOperation(value = "Current throughput of this stream on this node in messages per second")
    @Produces(MediaType.APPLICATION_JSON)
    public Response oneStreamThroughput(@ApiParam(name="streamId", required = true) @PathParam("streamId") String streamId) {
        final Map<String, Long> result = Maps.newHashMap();
        result.put("throughput", 0L);

        final HashMap<String,Counter> currentStreamThroughput = throughputStats.getCurrentStreamThroughput();
        if (currentStreamThroughput != null) {
            final Counter counter = currentStreamThroughput.get(streamId);
            if (counter != null && isPermitted(RestPermissions.STREAMS_READ, streamId))
                result.put("throughput", counter.get());
        }
        return Response.ok().entity(json(result)).build();
    }

    @GET @Timed
    @Path("/throughput")
    @ApiOperation("Current throughput of all visible streams on this node in messages per second")
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamThroughput() {
        Map<String, Map<String, Long>> result = Maps.newHashMap();
        final Map<String, Long> perStream = Maps.newHashMap();
        result.put("throughput", perStream);

        final HashMap<String,Counter> currentStreamThroughput = throughputStats.getCurrentStreamThroughput();
        if (currentStreamThroughput != null)
            for (Map.Entry<String, Counter> entry : currentStreamThroughput.entrySet())
                if (entry.getValue() != null && isPermitted(RestPermissions.STREAMS_READ, entry.getKey()))
                    perStream.put(entry.getKey(), entry.getValue().get());

        return Response.ok().entity(json(result)).build();
    }
}
