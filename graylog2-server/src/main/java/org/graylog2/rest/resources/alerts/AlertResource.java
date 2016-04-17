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
package org.graylog2.rest.resources.alerts;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.alerts.AlertImpl;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.streams.alerts.AlertListSummary;
import org.graylog2.rest.models.streams.alerts.AlertSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Alerts", description = "Manage stream alerts for all streams")
@Path("/streams/alerts")
public class AlertResource extends RestResource {
    private final StreamService streamService;
    private final AlertService alertService;

    @Inject
    public AlertResource(StreamService streamService,
                               AlertService alertService) {
        this.streamService = streamService;
        this.alertService = alertService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get the " + AlertImpl.MAX_LIST_COUNT + " most recent alarms of all streams.")
    @RequiresPermissions(RestPermissions.STREAMS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public AlertListSummary listAll(@ApiParam(name = "since", value = "Optional parameter to define a lower date boundary. (UNIX timestamp)", required = false)
                                    @QueryParam("since") int sinceTs) throws NotFoundException {
        final DateTime since;
        if (sinceTs > 0) {
            since = new DateTime(sinceTs * 1000L, DateTimeZone.UTC);
        } else {
            since = null;
        }

        List<AlertSummary> alerts = streamService.loadAll().stream()
                .filter(stream -> isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                .flatMap(stream -> alertService.loadRecentOfStream(stream.getId(), since).stream())
                .limit(AlertImpl.MAX_LIST_COUNT)
                .map(alert -> AlertSummary.create(alert.getId(), alert.getConditionId(), alert.getStreamId(), alert.getDescription(), alert.getConditionParameters(), alert.getTriggeredAt()))
                .collect(Collectors.toList());

        return AlertListSummary.create(alerts.size(), alerts);
    }
}
