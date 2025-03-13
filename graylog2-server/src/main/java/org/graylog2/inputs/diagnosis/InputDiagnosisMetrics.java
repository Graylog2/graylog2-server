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
package org.graylog2.inputs.diagnosis;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.graylog2.shared.metrics.MetricUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class InputDiagnosisMetrics {

    private static final int SIZE_FOR_15_MINUTES = (15 * 60) / InputDiagnosisMetricsPeriodical.UPDATE_FREQUENCY;
    private final Map<String, CircularFifoQueue<Long>> metrics = new HashMap<>();
    private final AtomicReference<MetricRegistry> localMetricRegistry = new AtomicReference<>(new MetricRegistry());
    private final MetricRegistry metricRegistry;

    @Inject
    public InputDiagnosisMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void incCount(String metricName) {
        localMetricRegistry.get().counter(metricName).inc();
    }

    void update() {
        MetricRegistry registry = localMetricRegistry.getAndSet(new MetricRegistry());
        registry.getCounters().forEach((metric, counter) ->
                metrics.compute(metric, (m, q) -> {
                    final CircularFifoQueue<Long> queue = Objects.requireNonNullElseGet(q, () -> new CircularFifoQueue<>(SIZE_FOR_15_MINUTES));
                    queue.add(counter.getCount());
                    MetricUtils.safelyRegister(metricRegistry, metric, (Gauge<Long>) () -> queue.stream()
                            .mapToLong(value -> value)
                            .sum());
                    return queue;
                }));

        metrics.entrySet().stream()
                .filter(e -> !registry.getCounters().containsKey(e.getKey()))
                .forEach(e -> e.getValue().add(0L));
    }
}
