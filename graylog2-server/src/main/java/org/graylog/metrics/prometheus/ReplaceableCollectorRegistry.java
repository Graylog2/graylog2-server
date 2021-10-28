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
package org.graylog.metrics.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Custom {@link CollectorRegistry} that delegates all read operations to a registry reference.
 *
 * This allows atomic collector registry replacements at runtime.
 *
 * The standard {@link CollectorRegistry} only supports a register/unregister workflow which opens up a potential
 * race condition. And since the {@link io.prometheus.client.exporter.HTTPServer} doesn't allow the replacement
 * of the collector registry without restarting the HTTP server, we need this custom implementation.
 *
 * Write operations are not supported and will throw an {@link UnsupportedOperationException}.
 */
public class ReplaceableCollectorRegistry extends CollectorRegistry {
    private final AtomicReference<CollectorRegistry> registryRef;

    public ReplaceableCollectorRegistry(AtomicReference<CollectorRegistry> registryRef) {
        this.registryRef = requireNonNull(registryRef, "registryRef cannot be null");
    }

    @Override
    public void register(Collector m) {
        throw new UnsupportedOperationException("The dynamic Prometheus collector registry doesn't support register()");
    }

    @Override
    public void unregister(Collector m) {
        throw new UnsupportedOperationException("The dynamic Prometheus collector registry doesn't support unregister()");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The dynamic Prometheus collector registry doesn't support clear()");
    }

    @Override
    public Enumeration<Collector.MetricFamilySamples> metricFamilySamples() {
        return registryRef.get().metricFamilySamples();
    }

    @Override
    public Enumeration<Collector.MetricFamilySamples> filteredMetricFamilySamples(Set<String> includedNames) {
        return registryRef.get().filteredMetricFamilySamples(includedNames);
    }

    @Override
    public Double getSampleValue(String name) {
        return registryRef.get().getSampleValue(name);
    }

    @Override
    public Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return registryRef.get().getSampleValue(name, labelNames, labelValues);
    }
}
