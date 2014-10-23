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
import org.graylog2.restclient.models.api.responses.metrics.RateMetricsResponse;

import java.text.DecimalFormat;
import java.util.Map;

public class Meter extends Metric {
    private static final DecimalFormat DF = new DecimalFormat("#.##");

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
        this(ImmutableMap.<String, Object>builder()
                .put("total", rate.total)
                .put("mean", rate.mean)
                .put("one_minute", rate.oneMinute)
                .put("five_minute", rate.fiveMinute)
                .put("fifteen_minute", rate.fifteenMinute)
                .build());
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
        return DF.format(mean);
    }

    public String getOneMinuteFormatted() {
        return DF.format(oneMinute);
    }

    public String getFiveMinuteFormatted() {
        return DF.format(fiveMinute);
    }

    public String getFifteenMinuteFormatted() {
        return DF.format(fifteenMinute);
    }
}
