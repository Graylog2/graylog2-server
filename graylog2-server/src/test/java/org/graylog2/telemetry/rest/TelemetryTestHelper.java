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
package org.graylog2.telemetry.rest;

import org.graylog2.system.traffic.TrafficCounterService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TelemetryTestHelper {
    public static final TrafficCounterService.TrafficHistogram TRAFFIC_HISTOGRAM = TrafficCounterService.TrafficHistogram.create(
            DateTime.now(DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC), Map.of(), Map.of(), Map.of());
    static final String CLUSTER = "cluster";
    static final String CURRENT_USER = "current_user";
    static final String USER_TELEMETRY_SETTINGS = "user_telemetry_settings";
    static final String LICENSE = "license";
    static final String PLUGIN = "plugin";
    static final String SEARCH_CLUSTER = "search_cluster";

    static final String DATA_NODES = "data_nodes";

    public static void mockTrafficData(TrafficCounterService trafficCounterService1) {
        when(trafficCounterService1.clusterTrafficOfLastDays(any(), any())).thenReturn(TelemetryTestHelper.TRAFFIC_HISTOGRAM);
    }
}
