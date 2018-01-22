/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

        try {
            //noinspection unused
            final Counter somename = MetricUtils.safelyRegister(metricRegistry, "somename", new Counter());
        } catch (Exception e) {
            assertTrue("Registering a metric with a different metric type fails on using it", e instanceof ClassCastException);
        }
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