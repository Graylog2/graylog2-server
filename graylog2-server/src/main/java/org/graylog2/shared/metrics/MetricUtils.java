/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.graylog2.rest.models.metrics.responses.RateMetricsResponse;
import org.graylog2.rest.models.metrics.responses.TimerMetricsResponse;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MetricUtils {
    private static final Logger log = LoggerFactory.getLogger(MetricUtils.class);

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
        final Map<String, Object> metricMap = Maps.newHashMap();
        metricMap.put("full_name", metricName);
        metricMap.put("name", metricName.substring(metricName.lastIndexOf(".") + 1));

        if (metric instanceof Timer) {
            metricMap.put("metric", buildTimerMap((Timer) metric));
            metricMap.put("type", "timer");
        } else if(metric instanceof Meter) {
            metricMap.put("metric", buildMeterMap((Meter) metric));
            metricMap.put("type", "meter");
        } else if(metric instanceof Histogram) {
            metricMap.put("metric", buildHistogramMap((Histogram) metric));
            metricMap.put("type", "histogram");
        } else if(metric instanceof Counter) {
            metricMap.put("metric", metric);
            metricMap.put("type", "counter");
        } else if(metric instanceof Gauge) {
            metricMap.put("metric", metric);
            metricMap.put("type", "gauge");
        } else {
            throw new IllegalArgumentException("Unknown metric type " + metric.getClass());
        }
        return metricMap;
    }

    public static TimerRateMetricsResponse buildTimerMap(Timer t) {
        final TimerRateMetricsResponse result = new TimerRateMetricsResponse();

        if (t == null) {
            return result;
        }

        final TimerMetricsResponse time = new TimerMetricsResponse();
        time.max = TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMax(), TimeUnit.NANOSECONDS);
        time.min = TimeUnit.MICROSECONDS.convert(t.getSnapshot().getMin(), TimeUnit.NANOSECONDS);
        time.mean = TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getMean(), TimeUnit.NANOSECONDS);
        time.percentile95th = TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get95thPercentile(), TimeUnit.NANOSECONDS);
        time.percentile98th = TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get98thPercentile(), TimeUnit.NANOSECONDS);
        time.percentile99th = TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().get99thPercentile(), TimeUnit.NANOSECONDS);
        time.stdDev = TimeUnit.MICROSECONDS.convert((long) t.getSnapshot().getStdDev(), TimeUnit.NANOSECONDS);

        final RateMetricsResponse rate = new RateMetricsResponse();
        rate.oneMinute = t.getOneMinuteRate();
        rate.fiveMinute = t.getFiveMinuteRate();
        rate.fifteenMinute = t.getFifteenMinuteRate();
        rate.total = t.getCount();
        rate.mean = t.getMeanRate();

        result.time = time;
        result.rate = rate;
        result.rateUnit = "events/second";
        result.durationUnit = TimeUnit.MICROSECONDS.toString().toLowerCase(Locale.ENGLISH);

        return result;
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

    public static MetricFilter filterSingleMetric(String name) {
        return new SingleMetricFilter(name);
    }

    public static <T extends Metric> T safelyRegister(MetricRegistry metricRegistry, String name, T metric) {
        try {
            return metricRegistry.register(name, metric);
        } catch (IllegalArgumentException ignored) {
            // safely ignore already existing metric, and simply return the one registered previously.
            // note that we do not guard against differing metric types here, we consider that a programming error for now.

            //noinspection unchecked
            return (T) metricRegistry.getMetrics().get(name);
        }
    }

    public static <T extends Metric> T getOrRegister(MetricRegistry metricRegistry, String name, T newMetric) {
        final Metric metric = metricRegistry.getMetrics().get(name);
        if (metric != null) {
            //noinspection unchecked
            return (T) metric;
        }
        try {
            return metricRegistry.register(name, newMetric);
        } catch (IllegalArgumentException ignored) {
            //noinspection unchecked
            return (T) metricRegistry.getMetrics().get(name);
        }
    }

    public static void safelyRegisterAll(MetricRegistry metricRegistry, MetricSet metrics) throws IllegalArgumentException {
        try {
            metricRegistry.registerAll(metrics);
        } catch (IllegalArgumentException e) {
            log.error("Duplicate metric set registered", e);
        }
    }

    public static Gauge<Long> constantGauge(final long constant) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() {
                return constant;
            }
        };
    }

    public static class SingleMetricFilter implements MetricFilter {
        private final String allowedName;
        public SingleMetricFilter(String allowedName) {
            this.allowedName = allowedName;
        }

        @Override
        public boolean matches(String name, Metric metric) {
            return allowedName.equals(name);
        }
    }
}
