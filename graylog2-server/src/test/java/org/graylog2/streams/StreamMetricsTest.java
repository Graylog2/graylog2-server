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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamMetricsTest {
    private MetricRegistry metricRegistry;
    private StreamMetrics streamMetrics;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
        streamMetrics = new StreamMetrics(metricRegistry);
    }

    @Test
    public void getExecutionTimer() {
        final Timer timer = streamMetrics.getExecutionTimer("stream-id", "stream-rule-id");

        assertThat(timer).isNotNull();
        assertThat(metricRegistry.getTimers())
                .containsKey("org.graylog2.plugin.streams.Stream.stream-id.StreamRule.stream-rule-id.executionTime");
    }
}