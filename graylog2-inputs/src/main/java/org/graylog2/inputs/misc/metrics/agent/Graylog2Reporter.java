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
package org.graylog2.inputs.misc.metrics.agent;

import com.codahale.metrics.*;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Graylog2Reporter extends ScheduledReporter {

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private String source;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.source = "metrics";
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder useSource(String source) {
            this.source = source;
            return this;
        }

        public Graylog2Reporter build(GELFTarget sender) {
            return new Graylog2Reporter(registry, sender, clock, rateUnit, durationUnit, filter, source);
        }
    }

    private final GELFTarget sender;
    private final String source;

    private Graylog2Reporter(MetricRegistry registry, GELFTarget sender, Clock clock, TimeUnit rateUnit,
                             TimeUnit durationUnit, MetricFilter filter, String source) {
        super(registry, "graylog2-reporter", filter, rateUnit, durationUnit);
        this.sender = sender;
        this.source = source;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        reportGauges(gauges);
        reportCounters(counters);
        reportHistograms(histograms);
        reportMeters(meters);
        reportTimers(timers);
    }

    private void reportMeters(SortedMap<String, Meter> meters) {
        for(Map.Entry<String, Meter> x : meters.entrySet()) {
            Meter meter = x.getValue();

            Map<String, Object> fields = Maps.newHashMap();
            fields.put("count", meter.getCount());
            fields.put("mean_rate", convertRate(meter.getMeanRate()));
            fields.put("one_minute_rate", convertRate(meter.getOneMinuteRate()));
            fields.put("five_minute_rate", convertRate(meter.getFiveMinuteRate()));
            fields.put("fifteen_minute_rate", convertRate(meter.getFifteenMinuteRate()));

            fields.put("name", x.getKey());
            fields.put("metrics_type", "meter");
            fields.put("type", "metrics");

            sender.deliver(buildShortMessage(x.getKey()), source, fields);
        }
    }

    private void reportGauges(SortedMap<String, Gauge> gauges) {
        for(Map.Entry<String, Gauge> x : gauges.entrySet()) {
            Gauge gauge = x.getValue();

            Map<String, Object> fields = Maps.newHashMap();
            fields.put("value", gauge.getValue());

            fields.put("name", x.getKey());
            fields.put("metrics_type", "gauge");
            fields.put("type", "metrics");

            sender.deliver(buildShortMessage(x.getKey()), source, fields);
        }
    }

    private void reportCounters(SortedMap<String, Counter> counters) {
        for(Map.Entry<String, Counter> x : counters.entrySet()) {
            Counter counter = x.getValue();

            Map<String, Object> fields = Maps.newHashMap();
            fields.put("count", counter.getCount());

            fields.put("name", x.getKey());
            fields.put("metrics_type", "counter");
            fields.put("type", "metrics");

            sender.deliver(buildShortMessage(x.getKey()), source, fields);
        }
    }

    private void reportHistograms(SortedMap<String, Histogram> histograms) {
        for(Map.Entry<String, Histogram> x : histograms.entrySet()) {
            Snapshot snapshot = x.getValue().getSnapshot();

            Map<String, Object> fields = Maps.newHashMap();
            fields.put("count", x.getValue().getCount());
            fields.put("75th_percentile", snapshot.get75thPercentile());
            fields.put("95th_percentile", snapshot.get95thPercentile());
            fields.put("98th_percentile", snapshot.get98thPercentile());
            fields.put("999th_percentile", snapshot.get999thPercentile());
            fields.put("99th_percentile", snapshot.get99thPercentile());
            fields.put("max", snapshot.getMax());
            fields.put("mean", snapshot.getMean());
            fields.put("median", snapshot.getMedian());
            fields.put("min", snapshot.getMin());
            fields.put("std_deviation", snapshot.getStdDev());

            fields.put("name", x.getKey());
            fields.put("metrics_type", "histogram");
            fields.put("type", "metrics");

            sender.deliver(buildShortMessage(x.getKey()), source, fields);
        }
    }

    private void reportTimers(SortedMap<String, Timer> timers) {
        for(Map.Entry<String, Timer> x : timers.entrySet()) {
            Timer timer = x.getValue();
            Snapshot snapshot = timer.getSnapshot();

            Map<String, Object> fields = Maps.newHashMap();
            fields.put("count", timer.getCount());
            fields.put("one_minute_rate", convertRate(timer.getOneMinuteRate()));
            fields.put("five_minute_rate", convertRate(timer.getFiveMinuteRate()));
            fields.put("fifteen_minute_rate", convertRate(timer.getFifteenMinuteRate()));
            fields.put("mean_rate", convertRate(timer.getMeanRate()));
            fields.put("75th_percentile", convertDuration(snapshot.get75thPercentile()));
            fields.put("95th_percentile", convertDuration(snapshot.get95thPercentile()));
            fields.put("98th_percentile", convertDuration(snapshot.get98thPercentile()));
            fields.put("999th_percentile", convertDuration(snapshot.get999thPercentile()));
            fields.put("99th_percentile", convertDuration(snapshot.get99thPercentile()));
            fields.put("max", convertDuration(snapshot.getMax()));
            fields.put("mean", convertDuration(snapshot.getMean()));
            fields.put("median", convertDuration(snapshot.getMedian()));
            fields.put("min", convertDuration(snapshot.getMin()));
            fields.put("std_deviation", convertDuration(snapshot.getStdDev()));

            fields.put("name", x.getKey());
            fields.put("metrics_type", "timer");
            fields.put("type", "metrics");

            sender.deliver(buildShortMessage(x.getKey()), source, fields);
        }
    }

    public String buildShortMessage(String name) {
        return "metrics";
    }

}
