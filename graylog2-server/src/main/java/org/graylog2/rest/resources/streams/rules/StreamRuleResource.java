/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.streams.rules;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.resources.streams.responses.SingleStreamRuleSummaryResponse;
import org.graylog2.rest.resources.streams.responses.StreamRuleListResponse;
import org.graylog2.rest.resources.streams.responses.StreamRuleTypeResponse;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.hibernate.validator.constraints.NotEmpty;

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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RequiresAuthentication
@Api(value = "StreamRules", description = "Manage stream rules")
@Path("/streams/{streamid}/rules")
public class StreamRuleResource extends RestResource {
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
                           @PathParam("streamid") String streamId,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);
        final StreamRule streamRule = streamRuleService.create(streamId, cr);
        final String id = streamService.save(streamRule);

        final SingleStreamRuleSummaryResponse response = SingleStreamRuleSummaryResponse.create(id);

        final URI streamRuleUri = getUriBuilderToSelf().path(StreamRuleResource.class)
                .path("{streamRuleId}")
                .build(streamId, id);

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
        streamRule = streamRuleService.load(streamRuleId);

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

        return streamRuleService.load(streamRuleId);
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

        final StreamRule streamRule = streamRuleService.load(streamRuleId);
        if (streamRule.getStreamId().equals(streamid)) {
            streamRuleService.destroy(streamRule);
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get all available stream types")
    @Produces(MediaType.APPLICATION_JSON)
    // TODO: Move this to a better place. This method is not related to a context that is bound to the instance of a stream.
    public List<StreamRuleTypeResponse> types(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                                          @PathParam("streamid") String streamid) {
        final List<StreamRuleTypeResponse> result = new ArrayList<>(StreamRuleType.values().length);
        for (StreamRuleType type : StreamRuleType.values()) {
            result.add(StreamRuleTypeResponse.create(type.getValue(), type.name(), type.getShortDesc(), type.getLongDesc()));
        }

        return result;
    }
}
