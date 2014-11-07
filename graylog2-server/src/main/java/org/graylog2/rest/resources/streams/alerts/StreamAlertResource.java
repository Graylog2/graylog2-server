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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertImpl;
import org.graylog2.alerts.AlertService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Alerts", description = "Manage stream alerts")
@Path("/streams/{streamId}/alerts")
public class StreamAlertResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertResource.class);

    private final StreamService streamService;
    private final AlertService alertService;

    private static final String CACHE_KEY_BASE = "alerts";

    private static final Cache<String, Map<String, Object>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(AlertImpl.REST_CHECK_CACHE_SECONDS, TimeUnit.SECONDS)
            .build();

    @Inject
    public StreamAlertResource(StreamService streamService,
                               AlertService alertService) {
        this.streamService = streamService;
        this.alertService = alertService;
    }

    @GET @Timed
    @ApiOperation(value = "Get the " + AlertImpl.MAX_LIST_COUNT + " most recent alarms of this stream.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response list(@ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
                         @ApiParam(name = "since", value = "Optional parameter to define a lower date boundary. (UNIX timestamp)", required = false) @QueryParam("since") int sinceTs) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        final DateTime since;
        if (sinceTs > 0) {
            since = new DateTime(sinceTs*1000L, DateTimeZone.UTC);
        } else {
            since = null;
        }

        List<Map<String,Object>> conditions = Lists.newArrayList();
        for(Alert alert : alertService.loadRecentOfStream(stream.getId(), since)) {
            conditions.add(alert.toMap());
        }

        long total = alertService.totalCountForStream(streamid);

        Map<String, Object> result = Maps.newHashMap();
        result.put("alerts", conditions);
        result.put("total", total);

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @GET @Timed
    @Path("check")
    @ApiOperation(value = "Check for triggered alert conditions of this streams. Results cached for " + AlertImpl.REST_CHECK_CACHE_SECONDS + " seconds.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response checkConditions(@ApiParam(name = "streamId", value = "The ID of the stream to check.", required = true) @PathParam("streamId") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        Map<String, Object> result;
        try {
            result = cache.get(CACHE_KEY_BASE + stream.getId(), new Callable<Map<String, Object>>() {
                @Override
                public Map<String, Object> call() throws Exception {
                    List<Map<String, Object>> results = Lists.newArrayList();
                    int triggered = 0;
                    for (AlertCondition alertCondition : streamService.getAlertConditions(stream)) {
                        Map<String, Object> conditionResult = Maps.newHashMap();
                        conditionResult.put("condition", alertService.asMap(alertCondition));

                        AlertCondition.CheckResult checkResult = alertService.triggeredNoGrace(alertCondition);
                        conditionResult.put("triggered", checkResult.isTriggered());

                        if (checkResult.isTriggered()) {
                            triggered++;
                            conditionResult.put("alert_description", checkResult.getResultDescription());
                        }

                        results.add(conditionResult);
                    }

                    Map<String, Object> result = Maps.newHashMap();
                    result.put("results", results);
                    result.put("calculated_at", Tools.getISO8601String(Tools.iso8601()));
                    result.put("total_triggered", triggered);

                    return result;
                }
            });
        } catch (ExecutionException e) {
            LOG.error("Could not check for alerts.", e);
            throw new WebApplicationException(500);
        }

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

}
