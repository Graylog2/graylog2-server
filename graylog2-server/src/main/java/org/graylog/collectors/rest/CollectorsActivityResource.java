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
package org.graylog.collectors.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;

@Tag(name = "Collectors/Activity")
@Path("/collectors/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@PublicCloudAPI
public class CollectorsActivityResource extends RestResource {

    private static final int RECENT_ACTIVITY_LIMIT = 20;

    private final FleetTransactionLogService transactionLogService;
    private final ActivityEntryMapper activityEntryMapper;

    @Inject
    public CollectorsActivityResource(FleetTransactionLogService transactionLogService,
                                      ActivityEntryMapper activityEntryMapper) {
        this.transactionLogService = transactionLogService;
        this.activityEntryMapper = activityEntryMapper;
    }

    @GET
    @Path("/recent")
    @Timed
    @Operation(summary = "Get recent activity across all fleets and collectors")
    @RequiresPermissions(CollectorsPermissions.ACTIVITIES_READ)
    public RecentActivityResponse recent() {
        final List<TransactionMarker> markers = transactionLogService.getRecentMarkers(RECENT_ACTIVITY_LIMIT);
        return new RecentActivityResponse(activityEntryMapper.toEntries(markers, this::isPermitted));
    }
}
