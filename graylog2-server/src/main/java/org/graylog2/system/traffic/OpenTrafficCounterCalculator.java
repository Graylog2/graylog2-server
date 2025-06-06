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
package org.graylog2.system.traffic;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import jakarta.inject.Inject;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTrafficCounterCalculator implements TrafficCounterCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTrafficCounterCalculator.class);
    private final NodeId nodeId;
    private final TrafficUpdater trafficUpdater;

    private volatile long previousInputBytes = 0L;
    private volatile long previousOutputBytes = 0L;
    private volatile long previousDecodedBytes = 0L;
    private final Counter inputCounter;
    private final Counter outputCounter;
    private final Counter decodedCounter;

    @Inject
    public OpenTrafficCounterCalculator(NodeId nodeId, TrafficUpdater trafficUpdater, MetricRegistry metricRegistry) {
        this.nodeId = nodeId;
        this.trafficUpdater = trafficUpdater;
        inputCounter = metricRegistry.counter(GlobalMetricNames.INPUT_TRAFFIC);
        outputCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        decodedCounter = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC);
    }


    @Override
    public void calculate(DateTime previousMinute) {
        LOG.trace("Calculating input and output traffic for the previous minute");

        final long currentInputBytes = inputCounter.getCount();
        final long currentOutputBytes = outputCounter.getCount();
        final long currentDecodedBytes = decodedCounter.getCount();

        final long inputLastMinute = currentInputBytes - previousInputBytes;
        previousInputBytes = currentInputBytes;
        final long outputBytesLastMinute = currentOutputBytes - previousOutputBytes;
        previousOutputBytes = currentOutputBytes;
        final long decodedBytesLastMinute = currentDecodedBytes - previousDecodedBytes;
        previousDecodedBytes = currentDecodedBytes;

        if (LOG.isDebugEnabled()) {
            final Size in = Size.bytes(inputLastMinute);
            final Size out = Size.bytes(outputBytesLastMinute);
            final Size decoded = Size.bytes(decodedBytesLastMinute);
            LOG.debug("Traffic in the last minute: in: {} bytes ({} MB), out: {} bytes ({} MB}), decoded: {} bytes ({} MB})",
                    in, in.toMegabytes(), out, out.toMegabytes(), decoded, decoded.toMegabytes());
        }
        trafficUpdater.updateTraffic(previousMinute,
                nodeId,
                inputLastMinute,
                outputBytesLastMinute,
                decodedBytesLastMinute);
    }
}
