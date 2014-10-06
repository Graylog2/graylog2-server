/*
 * Copyright 2013 TORCH UG
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
 */
package org.graylog2.restclient.lib.metrics;

import org.graylog2.restclient.models.api.responses.metrics.RateMetricsResponse;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Meter extends Metric {

    DecimalFormat df = new DecimalFormat("#.##");

    public final double total;
    public final double mean;
    public final double oneMinute;
    public final double fiveMinute;
    public final double fifteenMinute;

    public Meter(Map<String, Object> metric) {
        super(MetricType.METER);

        this.total = (double) metric.get("total");
        this.mean = (double) metric.get("mean");
        this.oneMinute = (double) metric.get("one_minute");
        this.fiveMinute = (double) metric.get("five_minute");
        this.fifteenMinute = (double) metric.get("fifteen_minute");
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
