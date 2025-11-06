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
import jakarta.inject.Singleton;
import org.graylog2.streams.OutputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class OutputsMetricsSupplier implements TelemetryMetricSupplier {
    private final OutputService outputService;

    @Inject
    public OutputsMetricsSupplier(OutputService outputService) {
        this.outputService = outputService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        //Converting the result of "countByType" to Map<String, Object>:
        final Map<String, Object> countByType = new HashMap<>(TypeFormatter.formatAll(outputService.countByType()));

        return Optional.of(TelemetryEvent.of(countByType));
    }

}
