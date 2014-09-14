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
package org.graylog2.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MetricUtils {

    public static Map<String, Object> mapAll(Map<String, Metric> metrics) {
        return mapAllFiltered(metrics, null);
    }

    public static Map<String, Object> mapAllFiltered(Map<String, Metric> metrics, Set<String> blacklist) {
        Map<String, Object> result = Maps.newHashMap();

        if (metrics == null) {
            return result;
        }

        for (Map.Entry<String, Metric> metric : metrics.entrySet()) {
            boolean filtered = false;
            if (blacklist != null) {
                for(String x : blacklist) {
                    if (metric.getKey().startsWith(x)) {
                        filtered = true;
                        break;
                    }
                }
            }

            if (filtered) {
                continue;
            }

            result.put(metric.getKey(), map(metric.getKey(), metric.getValue()));
        }

        return result;
    }

    public static Map<String, Object> map(String metricName, Metric metric) {
        String type = metric.getClass().getSimpleName().toLowerCase();

        if (type.isEmpty()) {
            type = "gauge";
        }

        Map<String, Object> metricMap = Maps.newHashMap();
        metricMap.put("full_name", metricName);
        metricMap.put("name", metricName.substring(metricName.lastIndexOf(".") + 1));
        metricMap.put("type", type);

        if (metric instanceof Timer) {
            metricMap.put("metric", buildTimerMap((Timer) metric));
        } else if(metric instanceof Meter) {
            metricMap.put("metric", buildMeterMap((Meter) metric));
        } else if(metric instanceof Histogram) {
            metricMap.put("metric", buildHistogramMap((Histogram) metric));
        } else {
            metricMap.put("metric", metric);
        }
        return metricMap;
    }

    public static Map<String, Object> buildTimerMap(Timer t) {
        Map<String, Object> metrics = Maps.newHashMap();

        if (t == null) {
            return metrics;
        }

        TimeUnit timeUnit = TimeUnit.MICROSECONDS;

        Map<String, Object> time = Maps.newHashMap();
        time.put("max", TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMax(), TimeUnit.NANOSECONDS));
        time.put("min", TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMin(), TimeUnit.NANOSECONDS));
        time.put("mean", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getMean(), TimeUnit.NANOSECONDS));
        time.put("95th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get95thPercentile(), TimeUnit.NANOSECONDS));
        time.put("98th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get98thPercentile(), TimeUnit.NANOSECONDS));
        time.put("99th_percentile", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get99thPercentile(), TimeUnit.NANOSECONDS));
        time.put("std_dev", TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getStdDev(), TimeUnit.NANOSECONDS));

        Map<String, Object> rate = Maps.newHashMap();
        rate.put("one_minute", t.getOneMinuteRate());
        rate.put("five_minute", t.getFiveMinuteRate());
        rate.put("fifteen_minute", t.getFifteenMinuteRate());
        rate.put("total", t.getCount());
        rate.put("mean", t.getMeanRate());

        metrics.put("rate_unit", "events/second");
        metrics.put("duration_unit", timeUnit.toString().toLowerCase());
        metrics.put("time", time);
        metrics.put("rate", rate);

        return metrics;
    }

    public static Map<String, Object> buildHistogramMap(Histogram h) {
        Map<String, Object> metrics = Maps.newHashMap();

        if (h == null) {
            return metrics;
        }

        Map<String, Object> time = Maps.newHashMap();
        time.put("max", h.getSnapshot().getMax());
        time.put("min", h.getSnapshot().getMin());
        time.put("mean", (long) h.getSnapshot().getMean());
        time.put("95th_percentile", (long) h.getSnapshot().get95thPercentile());
        time.put("98th_percentile", (long) h.getSnapshot().get98thPercentile());
        time.put("99th_percentile", (long) h.getSnapshot().get99thPercentile());
        time.put("std_dev", (long) h.getSnapshot().getStdDev());

        metrics.put("time", time);
        metrics.put("count", h.getCount());

        return metrics;
    }

    public static Map<String, Object> buildMeterMap(Meter m) {
        Map<String, Object> metrics = Maps.newHashMap();

        if (m == null) {
            return metrics;
        }

        Map<String, Object> rate = Maps.newHashMap();
        rate.put("one_minute", m.getOneMinuteRate());
        rate.put("five_minute", m.getFiveMinuteRate());
        rate.put("fifteen_minute", m.getFifteenMinuteRate());
        rate.put("total", m.getCount());
        rate.put("mean", m.getMeanRate());

        metrics.put("rate_unit", "events/second");
        metrics.put("rate", rate);

        return metrics;
    }

}
