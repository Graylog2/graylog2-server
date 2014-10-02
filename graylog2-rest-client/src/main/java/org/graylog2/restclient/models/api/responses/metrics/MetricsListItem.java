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
package org.graylog2.restclient.models.api.responses.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.lib.metrics.Gauge;
import org.graylog2.restclient.lib.metrics.Histogram;
import org.graylog2.restclient.lib.metrics.Meter;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.lib.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MetricsListItem {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsListItem.class);

    public String name;
    public String type;

    @JsonProperty("full_name")
    public String fullName;

    Map<String, Object> metric;

    @SuppressWarnings("unchecked")
    public Metric getMetric() {
        Metric.MetricType metricType = Metric.MetricType.valueOf(this.type.toUpperCase());

        try {
            switch (metricType) {
                case TIMER:
                    String timerTimeUnit = (String) metric.get("duration_unit");
                    Map<String, Object> timing = (Map<String, Object>) metric.get("time");
                    return new Timer(timing, Timer.Unit.valueOf(timerTimeUnit.toUpperCase()));
                case METER:
                    Map<String, Object> rate = (Map<String, Object>) metric.get("rate");
                    return new Meter(rate);
                case GAUGE:
                    return new Gauge(metric.get("value"));
                case HISTOGRAM:
                    Map<String, Object> histoTiming = (Map<String, Object>) metric.get("time");
                    double count = ((Number) metric.get("count")).doubleValue();
                    return new Histogram(histoTiming, count);
            }
        } catch(Exception e) {
            LOG.error("Could not parse metric: " + metric.toString(), e);
            throw new RuntimeException("Could not map metric to type. (more information in log) Type was: [" + this.type + "]", e);
        }

        throw new RuntimeException("No such metric type recognized: [" + this.type + "]");
    }

}
