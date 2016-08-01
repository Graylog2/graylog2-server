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
package org.graylog2.rest.resources.streams;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.auditlog.Actions;
import org.graylog2.auditlog.jersey.AuditLog;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.alarmcallbacks.requests.AlertReceivers;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.graylog2.rest.models.streams.requests.UpdateStreamRequest;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;
import org.graylog2.rest.resources.streams.requests.CloneStreamRequest;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.rest.resources.streams.responses.StreamListResponse;
import org.graylog2.rest.resources.streams.responses.StreamResponse;
import org.graylog2.rest.resources.streams.responses.TestMatchResponse;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRouterEngine;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@RequiresAuthentication
@Api(value = "Streams", description = "Manage streams")
@Path("/streams")
public class StreamResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final StreamRouterEngine.Factory streamRouterEngineFactory;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final AlertService alertService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public StreamResource(StreamService streamService,
                          StreamRuleService streamRuleService,
                          StreamRouterEngine.Factory streamRouterEngineFactory,
                          AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                          AlertService alertService,
                          ClusterEventBus clusterEventBus) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.streamRouterEngineFactory = streamRouterEngineFactory;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.alertService = alertService;
        this.clusterEventBus = clusterEventBus;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a stream")
    @RequiresPermissions(RestPermissions.STREAMS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditLog(object = "stream", captureRequestEntity = true, captureResponseEntity = true)
    public Response create(@ApiParam(name = "JSON body", required = true) final CreateStreamRequest cr) throws ValidationException {
        // Create stream.
        final Stream stream = streamService.create(cr, getCurrentUser().getName());
        stream.setDisabled(true);

        final String id = streamService.save(stream);

        final List<CreateStreamRuleRequest> rules = firstNonNull(cr.rules(), Collections.<CreateStreamRuleRequest>emptyList());
        for (CreateStreamRuleRequest request : rules) {
            StreamRule streamRule = streamRuleService.create(id, request);
            streamRuleService.save(streamRule);
        }

        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));

        final Map<String, String> result = ImmutableMap.of("stream_id", id);
        final URI streamUri = getUriBuilderToSelf().path(StreamResource.class)
            .path("{streamId}")
            .build(id);

        return Response.created(streamUri).entity(result).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all streams")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamListResponse get() {
        final List<Stream> allStreams = streamService.loadAll();
        final List<Stream> streams = new ArrayList<>(allStreams.size());
        for (Stream stream : allStreams) {
            if (isPermitted(RestPermissions.STREAMS_READ, stream.getId())) {
                streams.add(stream);
            }
        }

        return StreamListResponse.create(streams.size(), streams.stream().map(this::streamToResponse).collect(Collectors.toSet()));
    }

    @GET
    @Path("/enabled")
    @Timed
    @ApiOperation(value = "Get a list of all enabled streams")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamListResponse getEnabled() throws NotFoundException {
        final List<Stream> enabledStreams = streamService.loadAllEnabled();
        final List<Stream> streams = new ArrayList<>(enabledStreams.size());
        for (Stream stream : enabledStreams) {
            if (isPermitted(RestPermissions.STREAMS_READ, stream.getId())) {
                streams.add(stream);
            }
        }

        return StreamListResponse.create(streams.size(), streams.stream().map(this::streamToResponse).collect(Collectors.toSet()));
    }

    @GET
    @Path("/{streamId}")
    @Timed
    @ApiOperation(value = "Get a single stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public StreamResponse get(@ApiParam(name = "streamId", required = true)
                              @PathParam("streamId") @NotEmpty String streamId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        return streamToResponse(streamService.load(streamId));
    }

    @PUT
    @Timed
    @Path("/{streamId}")
    @ApiOperation(value = "Update a stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditLog(object = "stream", captureRequestEntity = true, captureResponseEntity = true)
    public StreamResponse update(@ApiParam(name = "streamId", required = true)
                                 @PathParam("streamId") String streamId,
                                 @ApiParam(name = "JSON body", required = true)
                                 @Valid @NotNull UpdateStreamRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        final Stream stream = streamService.load(streamId);

        if (!Strings.isNullOrEmpty(cr.title())) {
            stream.setTitle(cr.title());
        }

        if (!Strings.isNullOrEmpty(cr.description())) {
            stream.setDescription(cr.description());
        }

        if (cr.matchingType() != null) {
            try {
                stream.setMatchingType(Stream.MatchingType.valueOf(cr.matchingType()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid matching type '" + cr.matchingType()
                    + "' specified. Should be one of: " + Arrays.toString(Stream.MatchingType.values()));
            }
        }

        streamService.save(stream);
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));

        return streamToResponse(stream);
    }

    @DELETE
    @Path("/{streamId}")
    @Timed
    @ApiOperation(value = "Delete a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditLog(object = "stream")
    public void delete(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);
        streamService.destroy(stream);
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @POST
    @Path("/{streamId}/pause")
    @Timed
    @ApiOperation(value = "Pause a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @AuditLog(action = Actions.STOP, object = "stream")
    public void pause(@ApiParam(name = "streamId", required = true)
                      @PathParam("streamId") @NotEmpty String streamId) throws NotFoundException, ValidationException {
        checkAnyPermission(new String[]{RestPermissions.STREAMS_CHANGESTATE, RestPermissions.STREAMS_EDIT}, streamId);

        final Stream stream = streamService.load(streamId);
        streamService.pause(stream);
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @POST
    @Path("/{streamId}/resume")
    @Timed
    @ApiOperation(value = "Resume a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @AuditLog(action = Actions.START, object = "stream")
    public void resume(@ApiParam(name = "streamId", required = true)
                       @PathParam("streamId") @NotEmpty String streamId) throws NotFoundException, ValidationException {
        checkAnyPermission(new String[]{RestPermissions.STREAMS_CHANGESTATE, RestPermissions.STREAMS_EDIT}, streamId);

        final Stream stream = streamService.load(streamId);
        streamService.resume(stream);
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @POST
    @Path("/{streamId}/testMatch")
    @Timed
    @ApiOperation(value = "Test matching of a stream against a supplied message")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    public TestMatchResponse testMatch(@ApiParam(name = "streamId", required = true)
                                       @PathParam("streamId") String streamId,
                                       @ApiParam(name = "JSON body", required = true)
                                       @NotNull Map<String, Map<String, Object>> serialisedMessage) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final Stream stream = streamService.load(streamId);
        // This is such a hack...
        final Map<String, Object> m = new HashMap<>(serialisedMessage.get("message"));
        final String timeStamp = firstNonNull((String) m.get(Message.FIELD_TIMESTAMP),
            DateTime.now(DateTimeZone.UTC).toString(ISODateTimeFormat.dateTime()));
        m.put(Message.FIELD_TIMESTAMP, Tools.dateTimeFromString(timeStamp));
        final Message message = new Message(m);

        final ExecutorService executor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("stream-" + streamId + "-test-match-%d")
                        .build()
        );
        final StreamRouterEngine streamRouterEngine = streamRouterEngineFactory.create(Lists.newArrayList(stream), executor);
        final List<StreamRouterEngine.StreamTestMatch> streamTestMatches = streamRouterEngine.testMatch(message);
        final StreamRouterEngine.StreamTestMatch streamTestMatch = streamTestMatches.get(0);

        final Map<String, Boolean> rules = Maps.newHashMap();

        for (Map.Entry<StreamRule, Boolean> match : streamTestMatch.getMatches().entrySet()) {
            rules.put(match.getKey().getId(), match.getValue());
        }

        return TestMatchResponse.create(streamTestMatch.isMatched(), rules);
    }

    @POST
    @Path("/{streamId}/clone")
    @Timed
    @ApiOperation(value = "Clone a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditLog(action = "cloned", object = "stream", captureRequestEntity = true, captureResponseEntity = true)
    public Response cloneStream(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId,
                                @ApiParam(name = "JSON body", required = true) @Valid @NotNull CloneStreamRequest cr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_CREATE);
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final Stream sourceStream = streamService.load(streamId);

        // Create stream.
        final Map<String, Object> streamData = Maps.newHashMap();
        streamData.put(StreamImpl.FIELD_TITLE, cr.title());
        streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description());
        streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, getCurrentUser().getName());
        streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC());
        streamData.put(StreamImpl.FIELD_MATCHING_TYPE, sourceStream.getMatchingType().toString());

        final Stream stream = streamService.create(streamData);
        streamService.pause(stream);

        final String id = streamService.save(stream);

        final List<StreamRule> sourceStreamRules = streamRuleService.loadForStream(sourceStream);
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

        for (AlertCondition alertCondition : streamService.getAlertConditions(sourceStream)) {
            streamService.addAlertCondition(stream, alertCondition);
        }

        for (AlarmCallbackConfiguration alarmCallbackConfiguration : alarmCallbackConfigurationService.getForStream(sourceStream)) {
            final CreateAlarmCallbackRequest request = new CreateAlarmCallbackRequest();
            request.type = alarmCallbackConfiguration.getType();
            request.configuration = alarmCallbackConfiguration.getConfiguration();
            final AlarmCallbackConfiguration alarmCallback = alarmCallbackConfigurationService.create(stream.getId(), request, getCurrentUser().getName());
            alarmCallbackConfigurationService.save(alarmCallback);
        }

        for (Map.Entry<String, List<String>> entry : sourceStream.getAlertReceivers().entrySet()) {
            for (String receiver : entry.getValue()) {
                streamService.addAlertReceiver(stream, entry.getKey(), receiver);
            }
        }

        for (Output output : sourceStream.getOutputs()) {
            streamService.addOutput(stream, output);
        }

        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));

        final Map<String, String> result = ImmutableMap.of("stream_id", id);
        final URI streamUri = getUriBuilderToSelf().path(StreamResource.class)
            .path("{streamId}")
            .build(id);

        return Response.created(streamUri).entity(result).build();
    }

    private StreamResponse streamToResponse(Stream stream) {
        final List<String> emailAlertReceivers = stream.getAlertReceivers().get("emails");
        final List<String> usersAlertReceivers = stream.getAlertReceivers().get("users");
        final Collection<AlertConditionSummary> alertConditions = streamService.getAlertConditions(stream)
            .stream()
            .map((alertCondition) -> AlertConditionSummary.create(
                alertCondition.getId(),
                alertCondition.getTypeString(),
                alertCondition.getCreatorUserId(),
                alertCondition.getCreatedAt().toDate(),
                alertCondition.getParameters(),
                alertService.inGracePeriod(alertCondition),
                alertCondition.getTitle()))
            .collect(Collectors.toList());
        return StreamResponse.create(
            stream.getId(),
            (String)stream.getFields().get(StreamImpl.FIELD_CREATOR_USER_ID),
            outputsToSummaries(stream.getOutputs()),
            stream.getMatchingType().name(),
            stream.getDescription(),
            stream.getFields().get(StreamImpl.FIELD_CREATED_AT).toString(),
            stream.getDisabled(),
            stream.getStreamRules(),
            alertConditions,
            AlertReceivers.create(
                emailAlertReceivers == null ? Collections.emptyList() : emailAlertReceivers,
                usersAlertReceivers == null ? Collections.emptyList() : usersAlertReceivers
            ),
            stream.getTitle(),
            stream.getContentPack()
        );
    }

    private Collection<OutputSummary> outputsToSummaries(Collection<Output> outputs) {
        return outputs.stream()
            .map((output) -> OutputSummary.create(output.getId(),output.getTitle(), output.getType(),
                output.getCreatorUserId(), new DateTime(output.getCreatedAt()), output.getConfiguration(), output.getContentPack()))
            .collect(Collectors.toSet());
    }
}
