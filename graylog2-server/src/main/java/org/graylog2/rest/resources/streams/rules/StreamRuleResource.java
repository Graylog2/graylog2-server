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
package org.graylog2.rest.resources.streams.rules;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.responses.SingleStreamRuleSummaryResponse;
import org.graylog2.rest.resources.streams.responses.StreamRuleListResponse;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private final StreamRuleService streamRuleService;
    private final StreamService streamService;

    @Inject
    public StreamRuleResource(StreamRuleService streamRuleService,
                              StreamService streamService) {
        this.streamRuleService = streamRuleService;
        this.streamService = streamService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a stream rule")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true) CreateStreamRuleRequest cr) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            streamService.load(streamid);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }

        final StreamRule streamRule = streamRuleService.create(streamid, cr);

        final String id;
        try {
            id = streamService.save(streamRule);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException(e);
        }

        SingleStreamRuleSummaryResponse response = new SingleStreamRuleSummaryResponse();
        response.streamRuleId = id;

        return Response.status(Response.Status.CREATED).entity(response).build();
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
    public SingleStreamRuleSummaryResponse update(@ApiParam(name = "streamid", value = "The stream id this rule belongs to.", required = true) @PathParam("streamid") String streamid,
                           @ApiParam(name = "streamRuleId", value = "The stream rule id we are updating", required = true) @PathParam("streamRuleId") String streamRuleId,
                           @ApiParam(name = "JSON body", required = true) CreateStreamRuleRequest cr) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final StreamRule streamRule;
        try {
            streamRule = streamRuleService.load(loadObjectId(streamRuleId));
            if (!streamRule.getStreamId().equals(streamid)) {
                throw new NotFoundException();
            }
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }

        final StreamRuleType streamRuleType = StreamRuleType.fromInteger(cr.type);
        if(null == streamRuleType) {
            throw new BadRequestException("Unknown stream rule type " + cr.type);
        }

        streamRule.setField(cr.field);
        streamRule.setType(streamRuleType);
        streamRule.setInverted(cr.inverted);
        streamRule.setValue(cr.value);

        String id;
        try {
            streamRuleService.save(streamRule);
            id = streamRule.getId();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException(e);
        }

        SingleStreamRuleSummaryResponse response = new SingleStreamRuleSummaryResponse();
        response.streamRuleId = id;
        return response;
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamRuleListResponse get(@ApiParam(name = "streamid", value = "The id of the stream whose stream rules we want.", required = true) @PathParam("streamid") String streamid) {
        List<Map<String, Object>> streamRules = Lists.newArrayList();
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }

        StreamRuleListResponse response = new StreamRuleListResponse();
        try {
            response.streamRules = streamRuleService.loadForStream(stream);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream not found!");
        }
        response.total = response.streamRules.size();

        return response;
    }

    @GET @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Get a single stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamRule get(@ApiParam(name = "streamid", value = "The id of the stream whose stream rule we want.", required = true) @PathParam("streamid") String streamid,
                      @ApiParam(name = "streamRuleId", value = "The stream rule id we are getting", required = true) @PathParam("streamRuleId") String streamRuleId) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final StreamRule streamRule;
        try {
            streamRule = streamRuleService.load(loadObjectId(streamRuleId));
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return streamRule;
    }

    @DELETE @Path("/{streamRuleId}") @Timed
    @ApiOperation(value = "Delete a stream rule")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream rule not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true) @PathParam("streamid") String streamid,
                         @ApiParam(name = "streamRuleId", required = true) @PathParam("streamRuleId") String streamRuleId) {
        if (streamRuleId == null || streamRuleId.isEmpty()) {
            LOG.error("Missing streamRuleId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            StreamRule streamRule = streamRuleService.load(loadObjectId(streamRuleId));
            if (streamRule.getStreamId().equals(streamid)) {
                streamRuleService.destroy(streamRule);
            } else {
                throw new NotFoundException();
            }
        } catch (org.graylog2.database.NotFoundException e) {
            throw new javax.ws.rs.NotFoundException("Stream rule <" + streamRuleId + "> not found!");
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
