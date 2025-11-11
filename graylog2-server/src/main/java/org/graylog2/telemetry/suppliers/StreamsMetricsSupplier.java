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
package org.graylog2.telemetry.suppliers;

import jakarta.inject.Inject;
import org.graylog2.streams.StreamService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StreamsMetricsSupplier implements TelemetryMetricSupplier {
    private final StreamService streamService;

    @Inject
    public StreamsMetricsSupplier(StreamService streamService) {
        this.streamService = streamService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Map<String, Object> metrics = new HashMap<>(streamService.countBySource());

        return Optional.of(TelemetryEvent.of(metrics));
    }
}
