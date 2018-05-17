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