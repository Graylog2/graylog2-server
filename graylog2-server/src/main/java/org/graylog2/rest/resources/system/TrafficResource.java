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
package org.graylog2.rest.resources.system;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.traffic.TrafficCounterService;
import org.joda.time.Duration;

@PublicCloudAPI
@Tag(name = "System/ClusterTraffic", description = "Cluster traffic stats")
@RequiresAuthentication
@Path("/system/cluster/traffic")
@Produces(MediaType.APPLICATION_JSON)
public class TrafficResource extends RestResource {

    private final TrafficCounterService trafficCounterService;

    @Inject
    public TrafficResource(TrafficCounterService trafficCounterService) {
        this.trafficCounterService = trafficCounterService;
    }

    @GET
    @Operation(summary = "Get the cluster traffic stats")
    public TrafficCounterService.TrafficHistogram get(@Parameter(name = "days", description = "For how many days the traffic stats should be returned")
                                                      @QueryParam("days") @DefaultValue("30") int days,
                                                      @Parameter(name = "daily", description = "Whether the traffic should be aggregate to daily values")
                                                      @QueryParam("daily") @DefaultValue("false") boolean daily,
                                                      @Parameter(name = "includeToday", description = "Whether the traffic should include up to the current date/time (in UTC).")
                                                      @QueryParam("includeToday") @DefaultValue("true") boolean includeToday) {
        return trafficCounterService.clusterTrafficOfLastDays(Duration.standardDays(days),
                daily ? TrafficCounterService.Interval.DAILY : TrafficCounterService.Interval.HOURLY,
                includeToday);
    }

    @GET
    @Path("/detailed")
    @Operation(summary = "Get the cluster traffic stats with 10-minute granularity")
    public TrafficCounterService.TrafficHistogram getDetailed(@Parameter(name = "days", description = "For how many days the traffic stats should be returned")
                                                              @QueryParam("days") @DefaultValue("30") int days,
                                                              @Parameter(name = "includeToday", description = "Whether the traffic should include up to the current date/time (in UTC).")
                                                              @QueryParam("includeToday") @DefaultValue("true") boolean includeToday) {
        return trafficCounterService.clusterTrafficOfLastDays(Duration.standardDays(days),
                TrafficCounterService.Interval.TEN_MINUTE,
                includeToday);
    }
}
