/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package lib.metrics;

import models.api.responses.metrics.TimerMetricsResponse;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Timing {

    public enum Unit {
        MICROSECONDS
    }

    private final long standardDeviation;
    private final long minimum;
    private final long maximum;
    private final long mean;
    private final long percentile95th;
    private final long percentile98th;
    private final long percentile99th;

    public Timing(TimerMetricsResponse t, Unit durationUnit) {
        if (!durationUnit.equals(Unit.MICROSECONDS)) {
            throw new RuntimeException("Extractor timings must be in microseconds.");
        }

        this.standardDeviation = t.stdDev;
        this.minimum = t.min;
        this.maximum = t.max;
        this.mean = t.mean;
        this.percentile95th = t.percentile95th;
        this.percentile98th = t.percentile98th;
        this.percentile99th = t.percentile99th;
    }

    public long getStandardDeviation() {
        return standardDeviation;
    }

    public long getMinimum() {
        return minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public long getMean() {
        return mean;
    }

    public long get95thPercentile() {
        return percentile95th;
    }

    public long get98thPercentile() {
        return percentile98th;
    }

    public long get99thPercentile() {
        return percentile99th;
    }

}