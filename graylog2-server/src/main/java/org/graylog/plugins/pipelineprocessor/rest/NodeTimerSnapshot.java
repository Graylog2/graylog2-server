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
package org.graylog.plugins.pipelineprocessor.rest;

import java.util.HashMap;
import java.util.Map;

final class NodeTimerSnapshot {
    private final Map<String, TimerReading> readings;

    private NodeTimerSnapshot(Map<String, TimerReading> readings) {
        this.readings = readings;
    }

    static NodeTimerSnapshot empty() {
        return new NodeTimerSnapshot(Map.of());
    }

    static Builder builder() {
        return new Builder();
    }

    double cost(String metricName) {
        final TimerReading reading = readings.get(metricName);
        if (reading == null) {
            return 0.0d;
        }
        // events/s × µs/event = µs of CPU per wall-clock second
        // (how much CPU time a rule consumes per real second of elapsed time).
        // Approximate: the rate is averaged over the last 15 minutes, the mean
        // duration over the last ~5 minutes. The mismatched windows mean absolute
        // values drift, relative rankings between rules stay reliable.
        return reading.fifteenMinuteRate * reading.meanMicroseconds;
    }

    boolean isEmpty() {
        return readings.isEmpty();
    }

    static final class Builder {
        private final Map<String, TimerReading> readings = new HashMap<>();

        Builder timer(String name, double fifteenMinuteRate, double meanMicroseconds) {
            readings.put(name, new TimerReading(fifteenMinuteRate, meanMicroseconds));
            return this;
        }

        NodeTimerSnapshot build() {
            return new NodeTimerSnapshot(readings);
        }
    }

    private record TimerReading(double fifteenMinuteRate, double meanMicroseconds) {
    }
}
