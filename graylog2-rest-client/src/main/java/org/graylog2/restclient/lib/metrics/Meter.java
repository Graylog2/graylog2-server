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

import org.graylog2.restclient.models.api.responses.metrics.RateMetricsResponse;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Meter extends Metric {

    DecimalFormat df = new DecimalFormat("#.##");

    public final double total;
    public final double mean;
    public final double oneMinute;
    public final double fiveMinute;
    public final double fifteenMinute;

    public Meter(Map<String, Object> metric) {
        super(MetricType.METER);

        this.total = ((Number) metric.get("total")).doubleValue();
        this.mean = ((Number) metric.get("mean")).doubleValue();
        this.oneMinute = ((Number) metric.get("one_minute")).doubleValue();
        this.fiveMinute = ((Number) metric.get("five_minute")).doubleValue();
        this.fifteenMinute = ((Number) metric.get("fifteen_minute")).doubleValue();
    }

    public Meter(final RateMetricsResponse rate) {
        this(new HashMap<String, Object>() {{
            put("total", rate.total);
            put("mean", rate.mean);
            put("one_minute", rate.oneMinute);
            put("five_minute", rate.fiveMinute);
            put("fifteen_minute", rate.fifteenMinute);
        }});
    }

    public double getTotal() {
        return total;
    }

    public double getMean() {
        return mean;
    }

    public double getOneMinute() {
        return oneMinute;
    }

    public double getFiveMinute() {
        return fiveMinute;
    }

    public double getFifteenMinute() {
        return fifteenMinute;
    }

    public String getMeanFormatted() {
        return df.format(mean);
    }

    public String getOneMinuteFormatted() {
        return df.format(oneMinute);
    }

    public String getFiveMinuteFormatted() {
        return df.format(fiveMinute);
    }

    public String getFifteenMinuteFormatted() {
        return df.format(fifteenMinute);
    }

}
