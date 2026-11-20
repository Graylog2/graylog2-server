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
package org.graylog.events.processor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventProcessorExecutionMetricsTest {
    @Test
    void recordExecutionTimeRecordsWholeSeconds() {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final EventProcessorExecutionMetrics metrics = new EventProcessorExecutionMetrics(metricRegistry);
        final EventProcessor eventProcessor = mock(EventProcessor.class);

        metrics.recordExecutionTime(eventProcessor, "definition-id", Duration.ofMillis(7300));

        final Timer timer = metricRegistry.getTimers().values().iterator().next();
        assertThat(timer.getCount()).isEqualTo(1);
        assertThat(timer.getSnapshot().getMax()).isEqualTo(TimeUnit.MILLISECONDS.toNanos(7300));
    }

    @Test
    void recordExecutionTimeRecordsExactSeconds() {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final EventProcessorExecutionMetrics metrics = new EventProcessorExecutionMetrics(metricRegistry);
        final EventProcessor eventProcessor = mock(EventProcessor.class);

        metrics.recordExecutionTime(eventProcessor, "definition-id", Duration.ofSeconds(2));

        final Timer timer = metricRegistry.getTimers().values().iterator().next();
        assertThat(timer.getSnapshot().getMax()).isEqualTo(TimeUnit.SECONDS.toNanos(2));
    }
}
