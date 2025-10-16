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
package org.graylog2.outputs;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.streams.OutputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class OutputsMetricsSupplier implements TelemetryMetricSupplier {
    private final OutputService outputService;

    @Inject
    public OutputsMetricsSupplier(OutputService outputService) {
        this.outputService = outputService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        // Sorry, we need to convert the map, as "countByType" returns Map<String, Long>, while
        // the TelemetryEvent expects a Map<String, Object>:
        final Map<String, Object> countByType = outputService.countByType()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return countByType.isEmpty()
                ? Optional.empty()
                : Optional.of(TelemetryEvent.of(countByType));
    }
}
