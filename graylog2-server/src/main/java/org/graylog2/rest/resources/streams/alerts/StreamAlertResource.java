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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.streams.alerts.AlertListSummary;
import org.graylog2.rest.models.streams.alerts.AlertSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@RequiresAuthentication
@Api(value = "Stream/Alerts", description = "Manage stream legacy alerts for a given stream")
@Path("/streams/{streamId}/alerts")
public class StreamAlertResource extends RestResource {
    private static final int REST_CHECK_CACHE_SECONDS = 30;

    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertResource.class);
    private static final String CACHE_KEY_BASE = "alerts";
    private static final Cache<String, Map<String, Object>> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(REST_CHECK_CACHE_SECONDS, TimeUnit.SECONDS)
            .build();

    private final StreamService streamService;
    private final AlertService alertService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final AlarmCallbackFactory alarmCallbackFactory;

    @Inject
    public StreamAlertResource(StreamService streamService,
                               AlertService alertService,
                               AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                               AlarmCallbackFactory alarmCallbackFactory) {
        this.streamService = streamService;
        this.alertService = alertService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get the most recent alarms of this stream.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public AlertListSummary list(@ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
                                 @PathParam("streamId") String streamId,
                                 @ApiParam(name = "since", value = "Optional parameter to define a lower date boundary. (UNIX timestamp)")
                                 @QueryParam("since") @DefaultValue("0") @Min(0) int sinceTs,
                                 @ApiParam(name = "limit", value = "Maximum number of alerts to return.")
                                 @QueryParam("limit") @DefaultValue("300") @Min(1) int limit) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final DateTime since = new DateTime(sinceTs * 1000L, DateTimeZone.UTC);

        final Stream stream = streamService.load(streamId);
        final List<AlertSummary> conditions = toSummaryList(alertService.loadRecentOfStream(stream.getId(), since, limit));

        return AlertListSummary.create(alertService.totalCountForStream(streamId), conditions);
    }

    @GET
    @Timed
    @Path("paginated")
    @ApiOperation(value = "Get the alarms of this stream, filtered by specifying limit and offset parameters.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public AlertListSummary listPaginated(@ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
                                          @PathParam("streamId") String streamId,
                                          @ApiParam(name = "skip", value = "The number of elements to skip (offset).", required = true)
                                          @QueryParam("skip") @DefaultValue("0") int skip,
                                          @ApiParam(name = "limit", value = "The maximum number of elements to return.", required = true)
                                          @QueryParam("limit") @DefaultValue("300") int limit,
                                          @ApiParam(name = "state", value = "Alert state (resolved/unresolved)")
                                          @QueryParam("state") String state) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        Alert.AlertState alertState;
        try {
            alertState = Alert.AlertState.fromString(state);
        } catch (IllegalArgumentException e) {
            alertState = Alert.AlertState.ANY;
        }

        final Stream stream = streamService.load(streamId);
        final List<String> streamIdList = Lists.newArrayList(stream.getId());
        final List<AlertSummary> conditions = toSummaryList(alertService.listForStreamIds(streamIdList, alertState, skip, limit));

        return AlertListSummary.create(alertService.totalCountForStreams(streamIdList, alertState), conditions);
    }

    @GET
    @Timed
    @Path("check")
    @ApiOperation(value = "Check for triggered alert conditions of this streams. Results cached for " + REST_CHECK_CACHE_SECONDS + " seconds.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Map<String, Object> checkConditions(@ApiParam(name = "streamId", value = "The ID of the stream to check.", required = true)
                                               @PathParam("streamId") String streamId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final Stream stream = streamService.load(streamId);

        final Map<String, Object> result;
        try {
            result = CACHE.get(CACHE_KEY_BASE + stream.getId(), () -> {
                final List<AlertCondition> alertConditions = streamService.getAlertConditions(stream);
                int triggered = 0;

                final List<Map<String, Object>> results = new ArrayList<>(alertConditions.size());
                for (AlertCondition alertCondition : alertConditions) {
                    final Map<String, Object> conditionResult = new HashMap<>();
                    conditionResult.put("condition", alertCondition);

                    final AlertCondition.CheckResult checkResult = alertCondition.runCheck();
                    conditionResult.put("triggered", checkResult.isTriggered());

                    if (checkResult.isTriggered()) {
                        triggered++;
                        conditionResult.put("alert_description", checkResult.getResultDescription());
                    }

                    results.add(conditionResult);
                }

                return ImmutableMap.of(
                        "results", results,
                        "calculated_at", Tools.getISO8601String(Tools.nowUTC()),
                        "total_triggered", triggered);
            });
        } catch (ExecutionException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            LOG.error("Could not check for alerts.", rootCause);
            throw new InternalServerErrorException(rootCause);
        }

        return result;
    }

    @POST
    @Timed
    @Path("receivers")
    @ApiOperation(value = "Add an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId."),
            @ApiResponse(code = 400, message = "Stream has no email alarm callbacks.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_RECEIVER_CREATE)
    @Deprecated
    public Response addReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
            @PathParam("streamId") String streamId,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to add as alert receiver.", required = true)
            @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true)
            @QueryParam("type") String type
    ) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);
        checkArgument(!Strings.isNullOrEmpty(entity));

        if (type == null || !"users".equals(type) && !"emails".equals(type)) {
            final String msg = "No such type: [" + type + "]";
            LOG.warn(msg);
            throw new BadRequestException(msg);
        }

        final Stream stream = streamService.load(streamId);

        // TODO What's the actual URI of the created resource?
        final URI streamAlertUri = getUriBuilderToSelf().path(StreamAlertResource.class).build(streamId);

        // Maybe the list already contains this receiver?
        if (stream.getAlertReceivers().containsKey(type)
                || stream.getAlertReceivers().get(type) != null
                && stream.getAlertReceivers().get(type).contains(entity)) {
            return Response.created(streamAlertUri).build();
        }

        streamService.addAlertReceiver(stream, type, entity);

        return Response.created(streamAlertUri).build();
    }

    @DELETE
    @Timed
    @Path("receivers")
    @ApiOperation(value = "Remove an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId."),
            @ApiResponse(code = 400, message = "Stream has no email alarm callbacks.")
    })
    @AuditEvent(type = AuditEventTypes.ALERT_RECEIVER_DELETE)
    @Deprecated
    public void removeReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamId,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to remove from alert receivers.", required = true) @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true) @QueryParam("type") String type) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        if (!"users".equals(type) && !"emails".equals(type)) {
            final String msg = "No such type: [" + type + "]";
            LOG.warn(msg);
            throw new BadRequestException(msg);
        }

        final Stream stream = streamService.load(streamId);
        streamService.removeAlertReceiver(stream, type, entity);
    }

    @POST
    @Timed
    @Path("sendDummyAlert")
    @ApiOperation(value = "Send a test mail for a given stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId."),
            @ApiResponse(code = 400, message = "Stream has no alarm callbacks")
    })
    @NoAuditEvent("only used to test alert emails")
    public void sendDummyAlert(@ApiParam(name = "streamId", value = "The stream id the test alert should be sent for.", required = true)
                               @PathParam("streamId") String streamId)
            throws TransportConfigurationException, EmailException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);

        final DummyAlertCondition dummyAlertCondition = new DummyAlertCondition(stream, null, Tools.nowUTC(), getSubject().getPrincipal().toString(), Collections.emptyMap(), "Test Alert");
        try {
            AbstractAlertCondition.CheckResult checkResult = dummyAlertCondition.runCheck();
            List<AlarmCallbackConfiguration> callConfigurations = alarmCallbackConfigurationService.getForStream(stream);
            if (callConfigurations.size() == 0) {
                final String message = "Stream has no alarm callbacks, cannot send test alert.";
                LOG.warn(message);
                throw new BadRequestException(message);
            }

            for (AlarmCallbackConfiguration configuration : callConfigurations) {
                AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);
                alarmCallback.call(stream, checkResult);
            }
        } catch (AlarmCallbackException | ClassNotFoundException | AlarmCallbackConfigurationException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    private List<AlertSummary> toSummaryList(List<Alert> alertList) {
        return alertList.stream()
                .map(alert -> AlertSummary.create(
                        alert.getId(),
                        alert.getConditionId(),
                        alert.getStreamId(),
                        alert.getDescription(),
                        alert.getConditionParameters(),
                        alert.getTriggeredAt(),
                        alert.getResolvedAt(),
                        alert.isInterval()))
                .collect(Collectors.toList());
    }
}
