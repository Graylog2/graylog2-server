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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

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

}