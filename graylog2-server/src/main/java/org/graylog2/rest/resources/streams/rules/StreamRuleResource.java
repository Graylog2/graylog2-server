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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.responses.SingleStreamRuleSummaryResponse;
import org.graylog2.rest.resources.streams.responses.StreamRuleListResponse;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

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
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                           @PathParam("streamid") String streamid,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);
        final StreamRule streamRule = streamRuleService.create(streamid, cr);
        final String id = streamService.save(streamRule);

        final SingleStreamRuleSummaryResponse response = SingleStreamRuleSummaryResponse.create(id);

        final URI streamRuleUri = UriBuilder.fromResource(StreamRuleResource.class)
                .path("{streamRuleId}")
                .build(id);

        return Response.created(streamRuleUri).entity(response).build();
    }

    @PUT
    @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Update a stream rule")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream or stream rule not found."),
            @ApiResponse(code = 400, message = "Invalid JSON Body.")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SingleStreamRuleSummaryResponse update(@ApiParam(name = "streamid", value = "The stream id this rule belongs to.", required = true)
                                                  @PathParam("streamid") String streamid,
                                                  @ApiParam(name = "streamRuleId", value = "The stream rule id we are updating", required = true)
                                                  @PathParam("streamRuleId") String streamRuleId,
                                                  @ApiParam(name = "JSON body", required = true)
                                                  @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final StreamRule streamRule;
        streamRule = streamRuleService.load(loadObjectId(streamRuleId));

        if (!streamRule.getStreamId().equals(streamid)) {
            throw new NotFoundException();
        }

        final StreamRuleType streamRuleType = StreamRuleType.fromInteger(cr.type());
        if (null == streamRuleType) {
            throw new BadRequestException("Unknown stream rule type " + cr.type());
        }

        streamRule.setField(cr.field());
        streamRule.setType(streamRuleType);
        streamRule.setInverted(cr.inverted());
        streamRule.setValue(cr.value());

        streamRuleService.save(streamRule);

        return SingleStreamRuleSummaryResponse.create(streamRule.getId());
    }

    // TODO Remove after all consumers have been updated
    @POST
    @Path("/{streamRuleId}")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public SingleStreamRuleSummaryResponse updateDeprecated(@PathParam("streamid") String streamid,
                                                            @PathParam("streamRuleId") String streamRuleId,
                                                            @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        return update(streamid, streamRuleId, cr);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamRuleListResponse get(@ApiParam(name = "streamid", value = "The id of the stream whose stream rules we want.", required = true)
                                      @PathParam("streamid") String streamid) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final Stream stream = streamService.load(streamid);
        final List<StreamRule> streamRules = streamRuleService.loadForStream(stream);

        return StreamRuleListResponse.create(streamRules.size(), streamRules);
    }

    @GET
    @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Get a single stream rules")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamRule get(@ApiParam(name = "streamid", value = "The id of the stream whose stream rule we want.", required = true) @PathParam("streamid") String streamid,
                          @ApiParam(name = "streamRuleId", value = "The stream rule id we are getting", required = true) @PathParam("streamRuleId") String streamRuleId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        return streamRuleService.load(loadObjectId(streamRuleId));
    }

    @DELETE
    @Path("/{streamRuleId}")
    @Timed
    @ApiOperation(value = "Delete a stream rule")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream rule not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public void delete(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "streamRuleId", required = true)
                       @PathParam("streamRuleId") @NotEmpty String streamRuleId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final StreamRule streamRule = streamRuleService.load(loadObjectId(streamRuleId));
        if (streamRule.getStreamId().equals(streamid)) {
            streamRuleService.destroy(streamRule);
        } else {
            throw new NotFoundException();
        }
    }
}
