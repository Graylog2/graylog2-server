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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertImpl;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiresAuthentication
@Api(value = "Alerts", description = "Manage stream alerts")
@Path("/streams/{streamId}/alerts")
public class StreamAlertResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertResource.class);
    private static final String CACHE_KEY_BASE = "alerts";
    private static final Cache<String, Map<String, Object>> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(AlertImpl.REST_CHECK_CACHE_SECONDS, TimeUnit.SECONDS)
            .build();

    private final StreamService streamService;
    private final AlertService alertService;

    @Inject
    public StreamAlertResource(StreamService streamService,
                               AlertService alertService) {
        this.streamService = streamService;
        this.alertService = alertService;
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
                                    @PathParam("streamId") String streamid,
                                    @ApiParam(name = "since", value = "Optional parameter to define a lower date boundary. (UNIX timestamp)", required = false)
                                    @QueryParam("since") int sinceTs) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final DateTime since;
        if (sinceTs > 0) {
            since = new DateTime(sinceTs * 1000L, DateTimeZone.UTC);
        } else {
            since = null;
        }

        final Stream stream = streamService.load(streamid);
        final List<AlertSummary> conditions = toSummaryList(alertService.loadRecentOfStream(stream.getId(), since));

        return AlertListSummary.create(alertService.totalCountForStream(streamid), conditions);
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
                                    @PathParam("streamId") String streamid,
                                    @ApiParam(name = "skip", value = "The number of elements to skip (offset).", required = true)
                                    @QueryParam("skip") @DefaultValue("0") int skip,
                                    @ApiParam(name = "limit", value = "The maximum number of elements to return.", required = true)
                                    @QueryParam("limit") @DefaultValue("0") int limit) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        if (limit == 0) { limit = AlertImpl.MAX_LIST_COUNT; }

        final Stream stream = streamService.load(streamid);
        final List<AlertSummary> conditions = toSummaryList(alertService.listForStreamId(stream.getId(), skip, limit));

        return AlertListSummary.create(alertService.totalCountForStream(streamid), conditions);
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
                                               @PathParam("streamId") String streamid) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final Stream stream = streamService.load(streamid);

        final Map<String, Object> result;
        try {
            result = CACHE.get(CACHE_KEY_BASE + stream.getId(), new Callable<Map<String, Object>>() {
                @Override
                public Map<String, Object> call() throws Exception {
                    final List<Map<String, Object>> results = Lists.newArrayList();
                    int triggered = 0;

                    for (AlertCondition alertCondition : streamService.getAlertConditions(stream)) {
                        final Map<String, Object> conditionResult = Maps.newHashMap();
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
                }
            });
        } catch (ExecutionException e) {
            LOG.error("Could not check for alerts.", e);
            throw new InternalServerErrorException(e);
        }

        return result;
    }

    private List<AlertSummary> toSummaryList(List<Alert> alertList) {
        final List<AlertSummary> result = Lists.newArrayListWithCapacity(alertList.size());
        for (Alert alert : alertList) {
            result.add(AlertSummary.create(alert.getId(), alert.getConditionId(), alert.getStreamId(), alert.getDescription(), alert.getConditionParameters(), alert.getTriggeredAt()));
        }

        return result;
    }
}
