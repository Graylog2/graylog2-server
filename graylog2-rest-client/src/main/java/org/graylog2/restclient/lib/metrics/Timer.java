/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.lib.metrics;

import com.google.common.collect.ImmutableMap;
import org.graylog2.restclient.models.api.responses.metrics.TimerMetricsResponse;

import java.util.Map;

public class Timer extends Metric {
    public enum Unit {
        MICROSECONDS
    }

    private final double standardDeviation;
    private final double minimum;
    private final double maximum;
    private final double mean;
    private final double percentile95th;
    private final double percentile98th;
    private final double percentile99th;

    public Timer(Map<String, Object> timing, Unit durationUnit) {
        super(MetricType.TIMER);

        if (!durationUnit.equals(Unit.MICROSECONDS)) {
            throw new RuntimeException("Timings must be in microseconds.");
        }

        this.standardDeviation = ((Number) timing.get("std_dev")).doubleValue();
        this.minimum = ((Number) timing.get("min")).doubleValue();
        this.maximum = ((Number) timing.get("max")).doubleValue();
        this.mean = ((Number) timing.get("mean")).doubleValue();
        this.percentile95th = ((Number) timing.get("95th_percentile")).doubleValue();
        this.percentile98th = ((Number) timing.get("98th_percentile")).doubleValue();
        this.percentile99th = ((Number) timing.get("99th_percentile")).doubleValue();
    }

    public Timer(final TimerMetricsResponse t, Unit durationUnit) {
        this(ImmutableMap.<String, Object>builder()
                        .put("std_dev", t.stdDev)
                        .put("min", t.min)
                        .put("max", t.max)
                        .put("mean", t.mean)
                        .put("95th_percentile", t.percentile95th)
                        .put("98th_percentile", t.percentile98th)
                        .put("99th_percentile", t.percentile99th).build(),
                durationUnit);
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getMean() {
        return mean;
    }

    public double get95thPercentile() {
        return percentile95th;
    }

    public double get98thPercentile() {
        return percentile98th;
    }

    public double get99thPercentile() {
        return percentile99th;
    }
}