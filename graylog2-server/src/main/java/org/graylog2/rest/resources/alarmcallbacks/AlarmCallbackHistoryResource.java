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
package org.graylog2.rest.resources.alarmcallbacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackHistory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistoryService;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackHistoryListSummary;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackHistorySummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RequiresAuthentication
@Api(value = "AlarmCallbackHistories", description = "Manage stream legacy alarm callback histories")
@Path("/streams/{streamid}/alerts/{alertId}/history")
public class AlarmCallbackHistoryResource extends RestResource {
    private final AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Inject
    public AlarmCallbackHistoryResource(AlarmCallbackHistoryService alarmCallbackHistoryService) {
        this.alarmCallbackHistoryService = alarmCallbackHistoryService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks for this stream")
    @Produces(MediaType.APPLICATION_JSON)
    public AlarmCallbackHistoryListSummary getForAlert(@ApiParam(name = "streamid", value = "The id of the stream whose alarm callbacks history we want.", required = true)
                                                       @PathParam("streamid") String streamid,
                                                       @ApiParam(name = "alertId", value = "The id of the alert whose callback history we want.", required = true)
                                                       @PathParam("alertId") String alertId) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        final List<AlarmCallbackHistory> historyList = this.alarmCallbackHistoryService.getForAlertId(alertId);

        final List<AlarmCallbackHistorySummary> historySummaryList = Lists.newArrayListWithCapacity(historyList.size());
        for (AlarmCallbackHistory alarmCallbackHistory : historyList) {
            historySummaryList.add(AlarmCallbackHistorySummary.create(alarmCallbackHistory.id(),
                    alarmCallbackHistory.alarmcallbackConfiguration(),
                    alarmCallbackHistory.alertId(),
                    alarmCallbackHistory.alertConditionId(),
                    alarmCallbackHistory.result(),
                    alarmCallbackHistory.createdAt()));
        }

        return AlarmCallbackHistoryListSummary.create(historySummaryList);
    }
}
