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
import org.graylog2.indexer.IndexFailureCounter;
import org.graylog2.plugin.Tools;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.Optional;

/**
 * Reports the number of messages that failed to index over the last 24 hours, summed over all indices.
 */
public class IndexingFailuresMetricsSupplier implements TelemetryMetricSupplier {
    static final int WINDOW_HOURS = 24;

    private final IndexFailureCounter indexFailureCounter;

    @Inject
    public IndexingFailuresMetricsSupplier(IndexFailureCounter indexFailureCounter) {
        this.indexFailureCounter = indexFailureCounter;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        final long failures = indexFailureCounter.countByIndexSince(Tools.nowUTC().minusHours(WINDOW_HOURS))
                .values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return Optional.of(TelemetryEvent.of("indexing_failures_24h", failures));
    }
}
