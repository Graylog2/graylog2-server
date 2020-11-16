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
package org.graylog2.indexer.messages;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.GlobalMetricNames;

import javax.inject.Inject;

public class TrafficAccounting {
    private final Counter outputByteCounter;
    private final Counter systemTrafficCounter;

    @Inject
    public TrafficAccounting(MetricRegistry metricRegistry) {
        outputByteCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        systemTrafficCounter = metricRegistry.counter(GlobalMetricNames.SYSTEM_OUTPUT_TRAFFIC);
    }

    public void addOutputTraffic(long size) {
        this.outputByteCounter.inc(size);
    }

    public void addSystemTraffic(long size) {
        this.systemTrafficCounter.inc(size);
    }
}
