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
package org.graylog2.shared.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class MetricUtilsTest {

    @Test
    public void safelyRegister() {

        final MetricRegistry metricRegistry = new MetricRegistry();

        final Gauge<Long> longGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        };
        final Gauge<Long> newGauge = MetricUtils.safelyRegister(metricRegistry, "somename", longGauge);

        assertSame("metric objects are identical", longGauge, newGauge);

        try {
            MetricUtils.safelyRegister(metricRegistry, "somename", longGauge);
        } catch (Exception e) {
            fail("Should not have thrown: " + e.getMessage());
        }

        assertThatExceptionOfType(ClassCastException.class)
                .describedAs("Registering a metric with a different metric type fails on using it")
                .isThrownBy(() -> MetricUtils.safelyRegister(metricRegistry, "somename", new Counter()));
    }

    @Test
    public void getOrRegister() {
        final MetricRegistry metricRegistry = new MetricRegistry();

        final Counter newMetric1 = new Counter();
        final Counter newMetric2 = new Counter();

        assertThat(MetricUtils.getOrRegister(metricRegistry, "test1", newMetric1)).isEqualTo(newMetric1);
        assertThat(MetricUtils.getOrRegister(metricRegistry, "test1", newMetric2)).isEqualTo(newMetric1);
    }

    @Test
    public void mapSupportsCounter() {
        final Counter counter = new Counter();
        counter.inc(23L);

        final Map<String, Object> map = MetricUtils.map("metric", counter);
        assertThat(map)
                .containsEntry("type", "counter")
                .extracting("metric")
                .extracting("count")
                .containsExactly(23L);
    }

    @Test
    public void mapSupportsGauge() {
        final Gauge<Integer> gauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 23;
            }
        };

        final Map<String, Object> map = MetricUtils.map("metric", gauge);
        assertThat(map)
                .containsEntry("type", "gauge")
                .extracting("metric")
                .extracting("value")
                .containsExactly(23);
    }

    @Test
    public void mapSupportsGaugeLambda() {
        final Gauge<Integer> gauge = () -> 23;

        final Map<String, Object> map = MetricUtils.map("metric", gauge);
        assertThat(map)
                .containsEntry("type", "gauge")
                .extracting("metric")
                .extracting("value")
                .containsExactly(23);
    }

    @Test
    public void mapSupportsHdrHistogram() {
        final HdrHistogram histogram = new HdrHistogram(1000L, 0);
        histogram.update(23);

        final Map<String, Object> map = MetricUtils.map("metric", histogram);
        assertThat(map)
                .containsEntry("type", "histogram")
                .extracting("metric")
                .extracting("count")
                .containsExactly(1L);
    }

    @Test
    public void mapSupportsHistogram() {
        final Histogram histogram = new Histogram(new UniformReservoir());
        histogram.update(23);

        final Map<String, Object> map = MetricUtils.map("metric", histogram);
        assertThat(map)
                .containsEntry("type", "histogram")
                .extracting("metric")
                .extracting("count")
                .containsExactly(1L);
    }

    @Test
    public void mapSupportsMeter() {
        final Meter meter = new Meter();
        meter.mark();

        final Map<String, Object> map = MetricUtils.map("metric", meter);
        assertThat(map)
                .containsEntry("type", "meter")
                .extracting("metric")
                .extracting("rate")
                .extracting("total")
                .containsExactly(1L);
    }

    @Test
    public void mapSupportsTimer() {
        final TestClock clock = new TestClock();
        final Timer timer = new Timer(new UniformReservoir(), clock);
        try (Timer.Context time = timer.time()) {
            clock.setTick(5000L);
        }

        final Map<String, Object> map = MetricUtils.map("metric", timer);
        assertThat(map)
                .containsEntry("type", "timer")
                .extracting("metric")
                .extracting("rate")
                .extracting("total")
                .containsExactly(1.0D);
    }

    @Test
    public void mapThrowsIllegalArgumentExceptionForUnknownMetricType() {
        final Metric metric = new Metric() {};

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MetricUtils.map("metric", metric))
                .withMessageStartingWith("Unknown metric type class org.graylog2.shared.metrics.MetricUtilsTest");
    }

    private static class TestClock extends Clock {
        private long tick = 0L;

        @Override
        public long getTick() {
            return tick;
        }

        public void setTick(long tick) {
            this.tick = tick;
        }
    }
}
