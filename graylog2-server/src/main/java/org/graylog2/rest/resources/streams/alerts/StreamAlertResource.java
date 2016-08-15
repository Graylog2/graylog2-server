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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.common.Strings;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertImpl;
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.audit.AuditActions;
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
@Api(value = "Stream/Alerts", description = "Manage stream alerts for a given stream")
@Path("/streams/{streamId}/alerts")
public class StreamAlertResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertResource.class);
    private static final String CACHE_KEY_BASE = "alerts";
    private static final Cache<String, Map<String, Object>> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(AlertImpl.REST_CHECK_CACHE_SECONDS, TimeUnit.SECONDS)
            .build();

    private final StreamService streamService;
    private final AlertService alertService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final EmailAlarmCallback emailAlarmCallback;
    private final AlarmCallbackFactory alarmCallbackFactory;

    @Inject
    public StreamAlertResource(StreamService streamService,
                               AlertService alertService,
                               AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                               EmailAlarmCallback emailAlarmCallback,
                               AlarmCallbackFactory alarmCallbackFactory) {
        this.streamService = streamService;
        this.alertService = alertService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.emailAlarmCallback = emailAlarmCallback;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get the " + AlertImpl.MAX_LIST_COUNT + " most recent alarms of this stream.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public AlertListSummary list(@ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
                                 @PathParam("streamId") String streamId,
                                 @ApiParam(name = "since", value = "Optional parameter to define a lower date boundary. (UNIX timestamp)")
                                 @QueryParam("since") int sinceTs) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        final DateTime since;
        if (sinceTs > 0) {
            since = new DateTime(sinceTs * 1000L, DateTimeZone.UTC);
        } else {
            since = null;
        }

        final Stream stream = streamService.load(streamId);
        final List<AlertSummary> conditions = toSummaryList(alertService.loadRecentOfStream(stream.getId(), since));

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
                                          @QueryParam("limit") @DefaultValue("0") int limit) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamId);

        if (limit == 0) {
            limit = AlertImpl.MAX_LIST_COUNT;
        }

        final Stream stream = streamService.load(streamId);
        final List<AlertSummary> conditions = toSummaryList(alertService.listForStreamId(stream.getId(), skip, limit));

        return AlertListSummary.create(alertService.totalCountForStream(streamId), conditions);
    }

    @GET
    @Timed
    @Path("check")
    @ApiOperation(value = "Check for triggered alert conditions of this streams. Results cached for " + AlertImpl.REST_CHECK_CACHE_SECONDS + " seconds.")
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
                    conditionResult.put("condition", alertService.asMap(alertCondition));

                    final AlertCondition.CheckResult checkResult = alertService.triggeredNoGrace(alertCondition);
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
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(action = AuditActions.ALERT_RECEIVER_CREATE)
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

        if (type == null || (!type.equals("users") && !type.equals("emails"))) {
            final String msg = "No such type: [" + type + "]";
            LOG.warn(msg);
            throw new BadRequestException(msg);
        }

        final Stream stream = streamService.load(streamId);

        // TODO What's the actual URI of the created resource?
        final URI streamAlertUri = getUriBuilderToSelf().path(StreamAlertResource.class).build(streamId);

        // Maybe the list already contains this receiver?
        if (stream.getAlertReceivers().containsKey(type) || stream.getAlertReceivers().get(type) != null) {
            if (stream.getAlertReceivers().get(type).contains(entity)) {
                return Response.created(streamAlertUri).build();
            }
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
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditEvent(action = AuditActions.ALERT_RECEIVER_DELETE)
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
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @NoAuditEvent("only used to test alert emails")
    public void sendDummyAlert(@ApiParam(name = "streamId", value = "The stream id the dummy alert should be sent for.", required = true)
                               @PathParam("streamId") String streamId)
            throws TransportConfigurationException, EmailException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamId);

        final Stream stream = streamService.load(streamId);

        final DummyAlertCondition dummyAlertCondition = new DummyAlertCondition(stream, null, Tools.nowUTC(), getSubject().getPrincipal().toString(), Collections.emptyMap(), "Dummy Alert");
        try {
            AbstractAlertCondition.CheckResult checkResult = dummyAlertCondition.runCheck();
            List<AlarmCallbackConfiguration> callConfigurations = alarmCallbackConfigurationService.getForStream(stream);
            if (callConfigurations.size() > 0)
                for (AlarmCallbackConfiguration configuration : callConfigurations) {
                    AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);
                    alarmCallback.call(stream, checkResult);
                }
            else {
                emailAlarmCallback.call(stream, checkResult);
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
                        alert.getTriggeredAt()))
                .collect(Collectors.toList());
    }
}
