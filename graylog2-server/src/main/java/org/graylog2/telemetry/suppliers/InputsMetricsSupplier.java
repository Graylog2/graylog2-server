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
import org.graylog2.inputs.InputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InputsMetricsSupplier implements TelemetryMetricSupplier {
    private final InputService inputService;

    @Inject
    public InputsMetricsSupplier(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Map<String, Object> metrics = new HashMap<>(format(inputService.totalCountByType()));

        return Optional.of(TelemetryEvent.of((metrics)));
    }

    private Map<String, Long> format(Map<String, Long> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> TypeFormatter.format(e.getKey()),
                        Map.Entry::getValue,
                        Long::sum,
                        LinkedHashMap::new
                ));
    }
}
