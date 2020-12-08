/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.streams.rules;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
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

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
    @AuditEvent(type = AuditEventTypes.STREAM_RULE_CREATE)
    public Response create(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                           @PathParam("streamid") String streamId,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkNotDefaultStream(streamId, "Cannot add stream rules to the default stream.");

        // Check if stream exists
        streamService.load(streamId);

        final StreamRule streamRule = streamRuleService.create(streamId, cr);
        final String id = streamRuleService.save(streamRule);

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
    @AuditEvent(type = AuditEventTypes.STREAM_RULE_UPDATE)
    public SingleStreamRuleSummaryResponse update(@ApiParam(name = "streamid", value = "The stream id this rule belongs to.", required = true)
                                                  @PathParam("streamid") String streamid,
                                                  @ApiParam(name = "streamRuleId", value = "The stream rule id we are updating", required = true)
                                                  @PathParam("streamRuleId") String streamRuleId,
                                                  @ApiParam(name = "JSON body", required = true)
                                                  @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);
        checkNotDefaultStream(streamid, "Cannot update stream rules on default stream.");

        final StreamRule streamRule;
        streamRule = streamRuleService.load(streamRuleId);

        if (!streamRule.getStreamId().equals(streamid)) {
            throw new NotFoundException("Couldn't update stream rule " + streamRuleId + "in stream " + streamid);
        }

        final StreamRuleType streamRuleType = StreamRuleType.fromInteger(cr.type());
        if (null == streamRuleType) {
            throw new BadRequestException("Unknown stream rule type " + cr.type());
        }

        streamRule.setField(cr.field());
        streamRule.setType(streamRuleType);
        streamRule.setInverted(cr.inverted());
        streamRule.setValue(cr.value());
        streamRule.setDescription(cr.description());

        streamRuleService.save(streamRule);

        return SingleStreamRuleSummaryResponse.create(streamRule.getId());
    }

    // TODO Remove after all consumers have been updated
    @POST
    @Path("/{streamRuleId}")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.STREAM_RULE_UPDATE)
    @Deprecated
    public SingleStreamRuleSummaryResponse updateDeprecated(@PathParam("streamid") String streamid,
                                                            @PathParam("streamRuleId") String streamRuleId,
                                                            @Valid @NotNull CreateStreamRuleRequest cr) throws NotFoundException, ValidationException {
        checkNotDefaultStream(streamid, "Cannot remove stream rule from default stream.");
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
    @AuditEvent(type = AuditEventTypes.STREAM_RULE_DELETE)
    public void delete(@ApiParam(name = "streamid", value = "The stream id this new rule belongs to.", required = true)
                       @PathParam("streamid") String streamid,
                       @ApiParam(name = "streamRuleId", required = true)
                       @PathParam("streamRuleId") @NotEmpty String streamRuleId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);
        checkNotDefaultStream(streamid, "Cannot delete stream rule from default stream.");

        final StreamRule streamRule = streamRuleService.load(streamRuleId);
        if (streamRule.getStreamId().equals(streamid)) {
            streamRuleService.destroy(streamRule);
        } else {
            throw new NotFoundException("Couldn't delete stream rule " + streamRuleId + "in stream " + streamid);
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

    private void checkNotDefaultStream(String streamId, String message) {
        if (Stream.DEFAULT_STREAM_ID.equals(streamId)) {
            throw new BadRequestException(message);
        }
    }
}
