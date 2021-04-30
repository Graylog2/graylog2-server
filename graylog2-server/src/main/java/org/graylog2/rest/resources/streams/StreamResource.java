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
package org.graylog2.rest.resources.streams;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.graylog.security.UserContext;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.alarmcallbacks.requests.AlertReceivers;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.graylog2.rest.models.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.rest.models.streams.requests.UpdateStreamRequest;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;
import org.graylog2.rest.resources.streams.requests.CloneStreamRequest;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.rest.resources.streams.responses.StreamListResponse;
import org.graylog2.rest.resources.streams.responses.StreamPageListResponse;
import org.graylog2.rest.resources.streams.responses.StreamResponse;
import org.graylog2.rest.resources.streams.responses.TestMatchResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.PaginatedStreamService;
import org.graylog2.streams.StreamDTO;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRouterEngine;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@RequiresAuthentication
@Api(value = "Streams", description = "Manage streams")
@Path("/streams")
public class StreamResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(StreamDTO.FIELD_ID, SearchQueryField.Type.ID))
            .put(StreamDTO.FIELD_TITLE, SearchQueryField.create(StreamDTO.FIELD_TITLE))
            .put(StreamDTO.FIELD_DESCRIPTION, SearchQueryField.create(StreamDTO.FIELD_DESCRIPTION))
            .build();

    private final PaginatedStreamService paginatedStreamService;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final StreamRouterEngine.Factory streamRouterEngineFactory;
    private final IndexSetRegistry indexSetRegistry;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final AlertService alertService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public StreamResource(StreamService streamService,
                          PaginatedStreamService paginatedStreamService,
                          StreamRuleService streamRuleService,
                          StreamRouterEngine.Factory streamRouterEngineFactory,
                          IndexSetRegistry indexSetRegistry,
                          AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                          AlertService alertService) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.streamRouterEngineFactory = streamRouterEngineFactory;
        this.indexSetRegistry = indexSetRegistry;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.alertService = alertService;
        this.paginatedStreamService = paginatedStreamService;
        this.searchQueryParser = new SearchQueryParser(StreamImpl.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a stream")
    @RequiresPermissions(RestPermissions.STREAMS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.STREAM_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true) final CreateStreamRequest cr,
                           @Context UserContext userContext) throws ValidationException {
        // Create stream.
        final Stream stream = streamService.create(cr, getCurrentUser().getName());
        stream.setDisabled(true);

        if (!stream.getIndexSet().getConfig().isWritable()) {
            throw new BadRequestException("Assigned index set must be writable!");
        }

        final Set<StreamRule> streamRules = cr.rules().stream()
                .map(streamRule -> streamRuleService.create(null, streamRule))
                .collect(Collectors.toSet());
        final String id = streamService.saveWithRulesAndOwnership(stream, streamRules, userContext.getUser());

        final Map<String, String> result = ImmutableMap.of("stream_id", id);
        final URI streamUri = getUriBuilderToSelf().path(StreamResource.class)
            .path("{streamId}")
            .build(id);

        return Response.created(streamUri).entity(result).build();
    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation(value = "Get a paginated list of streams")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamPageListResponse getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
        @ApiParam(name = "sort",
                value = "The field to sort the result on",
                required = true,
                allowableValues = "title,description")
        @DefaultValue(StreamImpl.FIELD_TITLE) @QueryParam("sort") String sort,
        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
        @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final Predicate<StreamDTO> permissionFilter = streamDTO -> isPermitted(RestPermissions.STREAMS_READ, streamDTO.id());
        final PaginatedList<StreamDTO> result = paginatedStreamService
                .findPaginated(searchQuery, permissionFilter, page, perPage, sort, order);
        final List<String> streamIds = result.stream().map(streamDTO -> streamDTO.id()).collect(Collectors.toList());
        final Map<String, List<StreamRule>> streamRuleMap = streamRuleService.loadForStreamIds(streamIds);
        final List<StreamDTO> streams = result.stream().map(streamDTO -> {
            List<StreamRule> rules = streamRuleMap.getOrDefault(streamDTO.id(), Collections.emptyList());
            return streamDTO.toBuilder().rules(rules).build();
        }).collect(Collectors.toList());
        final long total = paginatedStreamService.count();
        final PaginatedList<StreamDTO> streamDTOS = new PaginatedList<>(streams,
                result.pagination().total(), result.pagination().page(), result.pagination().perPage());
        return StreamPageListResponse.create(query, streamDTOS.pagination(), total, sort, order, streams);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all streams")
    @Deprecated
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
    @AuditEvent(type = AuditEventTypes.STREAM_UPDATE)
    public StreamResponse update(@ApiParam(name = "streamId", required = true)
                                 @PathParam("streamId") String streamId,
                                 @ApiParam(name = "JSON body", required = true)
                                 @Valid @NotNull UpdateStreamRequest cr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkNotDefaultStream(streamId, "The default stream cannot be edited.");

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

        final Boolean removeMatchesFromDefaultStream = cr.removeMatchesFromDefaultStream();
        if(removeMatchesFromDefaultStream != null) {
            stream.setRemoveMatchesFromDefaultStream(removeMatchesFromDefaultStream);
        }

        // Apparently we are sending partial resources sometimes so do not overwrite the index set
        // id if it's null/empty in the update request.
        if (!Strings.isNullOrEmpty(cr.indexSetId())) {
            stream.setIndexSetId(cr.indexSetId());
        }

        final Optional<IndexSet> indexSet = indexSetRegistry.get(stream.getIndexSetId());

        if (!indexSet.isPresent()) {
            throw new BadRequestException("Index set with ID <" + stream.getIndexSetId() + "> does not exist!");
        } else if (!indexSet.get().getConfig().isWritable()) {
            throw new BadRequestException("Assigned index set must be writable!");
        }

        streamService.save(stream);

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
    @AuditEvent(type = AuditEventTypes.STREAM_DELETE)
    public void delete(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkNotDefaultStream(streamId, "The default stream cannot be deleted.");

        final Stream stream = streamService.load(streamId);
        streamService.destroy(stream);
    }

    @POST
    @Path("/{streamId}/pause")
    @Timed
    @ApiOperation(value = "Pause a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @AuditEvent(type = AuditEventTypes.STREAM_STOP)
    public void pause(@ApiParam(name = "streamId", required = true)
                      @PathParam("streamId") @NotEmpty String streamId) throws NotFoundException, ValidationException {
        checkAnyPermission(new String[]{RestPermissions.STREAMS_CHANGESTATE, RestPermissions.STREAMS_EDIT}, streamId);
        checkNotDefaultStream(streamId, "The default stream cannot be paused.");

        final Stream stream = streamService.load(streamId);
        streamService.pause(stream);
    }

    @POST
    @Path("/{streamId}/resume")
    @Timed
    @ApiOperation(value = "Resume a stream")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @AuditEvent(type = AuditEventTypes.STREAM_START)
    public void resume(@ApiParam(name = "streamId", required = true)
                       @PathParam("streamId") @NotEmpty String streamId) throws NotFoundException, ValidationException {
        checkAnyPermission(new String[]{RestPermissions.STREAMS_CHANGESTATE, RestPermissions.STREAMS_EDIT}, streamId);
        checkNotDefaultStream(streamId, "The default stream cannot be resumed.");

        final Stream stream = streamService.load(streamId);
        streamService.resume(stream);
    }

    @POST
    @Path("/{streamId}/testMatch")
    @Timed
    @ApiOperation(value = "Test matching of a stream against a supplied message")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Stream not found."),
        @ApiResponse(code = 400, message = "Invalid or missing Stream id.")
    })
    @NoAuditEvent("only used for testing stream matches")
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
    @AuditEvent(type = AuditEventTypes.STREAM_CREATE)
    public Response cloneStream(@ApiParam(name = "streamId", required = true) @PathParam("streamId") String streamId,
                                @ApiParam(name = "JSON body", required = true) @Valid @NotNull CloneStreamRequest cr,
                                @Context UserContext userContext) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_CREATE);
        checkPermission(RestPermissions.STREAMS_READ, streamId);
        checkNotDefaultStream(streamId, "The default stream cannot be cloned.");

        final Stream sourceStream = streamService.load(streamId);
        final String creatorUser = getCurrentUser().getName();

        final List<StreamRule> sourceStreamRules = streamRuleService.loadForStream(sourceStream);
        final ImmutableSet.Builder<StreamRule> newStreamRules = ImmutableSet.builderWithExpectedSize(sourceStreamRules.size());
        for (StreamRule streamRule : sourceStreamRules) {
            final Map<String, Object> streamRuleData = Maps.newHashMapWithExpectedSize(6);

            streamRuleData.put(StreamRuleImpl.FIELD_TYPE, streamRule.getType().toInteger());
            streamRuleData.put(StreamRuleImpl.FIELD_FIELD, streamRule.getField());
            streamRuleData.put(StreamRuleImpl.FIELD_VALUE, streamRule.getValue());
            streamRuleData.put(StreamRuleImpl.FIELD_INVERTED, streamRule.getInverted());
            streamRuleData.put(StreamRuleImpl.FIELD_DESCRIPTION, streamRule.getDescription());

            final StreamRule newStreamRule = streamRuleService.create(streamRuleData);
            newStreamRules.add(newStreamRule);
        }

        final Map<String, Object> streamData = Maps.newHashMap();
        streamData.put(StreamImpl.FIELD_TITLE, cr.title());
        streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description());
        streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, creatorUser);
        streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC());
        streamData.put(StreamImpl.FIELD_MATCHING_TYPE, sourceStream.getMatchingType().toString());
        streamData.put(StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, cr.removeMatchesFromDefaultStream());
        streamData.put(StreamImpl.FIELD_DISABLED, true);
        streamData.put(StreamImpl.FIELD_INDEX_SET_ID, cr.indexSetId());

        final Stream stream = streamService.create(streamData);
        final String savedStreamId = streamService.saveWithRulesAndOwnership(stream, newStreamRules.build(), userContext.getUser());
        final ObjectId savedStreamObjectId = new ObjectId(savedStreamId);

        for (AlertCondition alertCondition : streamService.getAlertConditions(sourceStream)) {
            try {
                final AlertCondition clonedAlertCondition = alertService.fromRequest(
                    CreateConditionRequest.create(alertCondition.getType(), alertCondition.getTitle(), alertCondition.getParameters()),
                    stream,
                    creatorUser
                );
                streamService.addAlertCondition(stream, clonedAlertCondition);
            } catch (ConfigurationException e) {
                LOG.warn("Unable to clone alert condition <" + alertCondition + "> - skipping: ", e);
            }
        }

        for (AlarmCallbackConfiguration alarmCallbackConfiguration : alarmCallbackConfigurationService.getForStream(sourceStream)) {
            final CreateAlarmCallbackRequest request = CreateAlarmCallbackRequest.create(alarmCallbackConfiguration);
            final AlarmCallbackConfiguration alarmCallback = alarmCallbackConfigurationService.create(stream.getId(), request, getCurrentUser().getName());
            alarmCallbackConfigurationService.save(alarmCallback);
        }

        final Set<ObjectId> outputIds = sourceStream.getOutputs().stream()
                .map(Output::getId)
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        streamService.addOutputs(savedStreamObjectId, outputIds);

        final Map<String, String> result = ImmutableMap.of("stream_id", savedStreamId);
        final URI streamUri = getUriBuilderToSelf().path(StreamResource.class)
            .path("{streamId}")
            .build(savedStreamId);

        return Response.created(streamUri).entity(result).build();
    }

    private StreamResponse streamToResponse(Stream stream) {
        final List<String> emailAlertReceivers = stream.getAlertReceivers().get("emails");
        final List<String> usersAlertReceivers = stream.getAlertReceivers().get("users");
        final Collection<AlertConditionSummary> alertConditions = streamService.getAlertConditions(stream)
            .stream()
            .map((alertCondition) -> AlertConditionSummary.createWithoutGrace(
                alertCondition.getId(),
                alertCondition.getType(),
                alertCondition.getCreatorUserId(),
                alertCondition.getCreatedAt().toDate(),
                alertCondition.getParameters(),
                alertCondition.getTitle()))
            .collect(Collectors.toList());
        return StreamResponse.create(
            stream.getId(),
            (String) stream.getFields().get(StreamImpl.FIELD_CREATOR_USER_ID),
            outputsToSummaries(stream.getOutputs()),
            stream.getMatchingType().name(),
            stream.getDescription(),
            stream.getFields().get(StreamImpl.FIELD_CREATED_AT).toString(),
            stream.getDisabled(),
            stream.getStreamRules(),
            alertConditions,
            AlertReceivers.create(emailAlertReceivers, usersAlertReceivers),
            stream.getTitle(),
            stream.getContentPack(),
            stream.isDefaultStream(),
            stream.getRemoveMatchesFromDefaultStream(),
            stream.getIndexSetId()
        );
    }

    private Collection<OutputSummary> outputsToSummaries(Collection<Output> outputs) {
        return outputs.stream()
            .map((output) -> OutputSummary.create(output.getId(),output.getTitle(), output.getType(),
                output.getCreatorUserId(), new DateTime(output.getCreatedAt()), output.getConfiguration(), output.getContentPack()))
            .collect(Collectors.toSet());
    }

    private void checkNotDefaultStream(String streamId, String message) {
        if (Stream.DEFAULT_STREAM_ID.equals(streamId)) {
            throw new BadRequestException(message);
        }
    }
}
