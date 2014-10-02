/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.lib.metrics;

import org.graylog2.restclient.models.api.responses.metrics.TimerMetricsResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
        this(new HashMap<String, Object>() {{
            put("std_dev", t.stdDev);
            put("min", t.min);
            put("max", t.max);
            put("mean", t.mean);
            put("95th_percentile", t.percentile95th);
            put("98th_percentile", t.percentile98th);
            put("99th_percentile", t.percentile99th);
        }}, durationUnit);
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