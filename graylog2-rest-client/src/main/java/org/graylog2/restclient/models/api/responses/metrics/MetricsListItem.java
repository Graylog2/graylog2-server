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
package org.graylog2.restclient.models.api.responses.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.lib.metrics.Counter;
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
                case COUNTER:
                    return new Counter(((Number) metric.get("count")).longValue());
            }
        } catch(Exception e) {
            LOG.error("Could not parse metric: " + metric.toString(), e);
            throw new RuntimeException("Could not map metric to type. (more information in log) Type was: [" + this.type + "]", e);
        }

        throw new RuntimeException("No such metric type recognized: [" + this.type + "]");
    }

}
