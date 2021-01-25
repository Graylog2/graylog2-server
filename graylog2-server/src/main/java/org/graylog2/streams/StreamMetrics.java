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
package org.graylog2.streams;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import javax.inject.Inject;
import java.util.Map;

public class StreamMetrics {
    private final MetricRegistry metricRegistry;

    private final Map<String, Meter> streamIncomingMeters = Maps.newHashMap();
    private final Map<String, Timer> streamExecutionTimers = Maps.newHashMap();
    private final Map<String, Meter> streamExceptionMeters = Maps.newHashMap();
    private final Map<String, Meter> streamRuleTimeoutMeters = Maps.newHashMap();
    private final Map<String, Meter> streamFaultsExceededMeters = Maps.newHashMap();


    @Inject
    public StreamMetrics(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void markIncomingMeter(String streamId) {
        getIncomingMeter(streamId).mark();
    }

    public Timer getExecutionTimer(String streamId, String streamRuleId) {
        Timer timer = this.streamExecutionTimers.get(streamRuleId);
        if (timer == null) {
            timer = metricRegistry.timer(MetricRegistry.name(Stream.class, streamId, "StreamRule", streamRuleId, "executionTime"));
            this.streamExecutionTimers.put(streamRuleId, timer);
        }

        return timer;
    }


    public void markExceptionMeter(String streamId) {
        getExceptionMeter(streamId).mark();
    }

    public void markStreamRuleTimeout(String streamId) {
        getStreamRuleTimeoutMeter(streamId).mark();
    }

    public void markStreamFaultsExceeded(String streamId) {
        getStreamFaultsExceededMeter(streamId).mark();
    }

    private Meter getIncomingMeter(String streamId) {
        Meter meter = this.streamIncomingMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "incomingMessages"));
            this.streamIncomingMeters.put(streamId, meter);
        }

        return meter;
    }

    private Meter getExceptionMeter(String streamId) {
        Meter meter = this.streamExceptionMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "matchingExceptions"));
            this.streamExceptionMeters.put(streamId, meter);
        }

        return meter;
    }

    private Meter getStreamRuleTimeoutMeter(final String streamId) {
        Meter meter = this.streamRuleTimeoutMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "ruleTimeouts"));
            this.streamRuleTimeoutMeters.put(streamId, meter);
        }

        return meter;
    }

    private Meter getStreamFaultsExceededMeter(final String streamId) {
        Meter meter = this.streamFaultsExceededMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "faultsExceeded"));
            this.streamFaultsExceededMeters.put(streamId, meter);
        }

        return meter;
    }
}
