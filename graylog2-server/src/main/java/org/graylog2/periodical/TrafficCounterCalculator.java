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
package org.graylog2.periodical;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.system.traffic.TrafficCounterService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TrafficCounterCalculator extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterCalculator.class);
    private final NodeId nodeId;
    private final TrafficCounterService trafficService;
    private final MetricRegistry metricRegistry;
    private long previousInputBytes = 0L;
    private long previousOutputBytes = 0L;
    private long previousDecodedBytes = 0L;
    private Counter inputCounter;
    private Counter outputCounter;
    private Counter decodedCounter;

    @Inject
    public TrafficCounterCalculator(NodeId nodeId, TrafficCounterService trafficService, MetricRegistry metricRegistry) {
        this.nodeId = nodeId;
        this.trafficService = trafficService;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void initialize() {
        inputCounter = metricRegistry.counter(GlobalMetricNames.INPUT_TRAFFIC);
        outputCounter = metricRegistry.counter(GlobalMetricNames.OUTPUT_TRAFFIC);
        decodedCounter = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC);
    }

    @Override
    public void doRun() {
        final DateTime now = Tools.nowUTC();
        final int secondOfMinute = now.getSecondOfMinute();
        // on the top of every minute, we flush the current throughput
        if (secondOfMinute == 0) {
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
            final DateTime previousMinute = now.minusMinutes(1);
            trafficService.updateTraffic(previousMinute, nodeId, inputLastMinute, outputBytesLastMinute, decodedBytesLastMinute);
        }
    }


    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
