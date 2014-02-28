/*
 * Copyright 2013 TORCH GmbH
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
package org.graylog2.metrics;

import com.codahale.metrics.*;
import com.google.common.collect.Lists;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class MongoDbMetricsReporter extends ScheduledReporter {
    private static final Logger log = LoggerFactory.getLogger(MongoDbMetricsReporter.class);

    private final Core core;
    private final Clock clock;
    private final String nodeId;

    private MongoDbMetricsReporter(Core core, MetricRegistry registry,
            Clock clock,
            TimeUnit rateUnit,
            TimeUnit durationUnit,
            MetricFilter filter) {
        super(registry, "mongodb-reporter", filter, rateUnit, durationUnit);
        this.core = core;
        nodeId = core.getNodeId();
        this.clock = clock;
    }

    public static Builder forRegistry(Core core, MetricRegistry registry) {
        return new Builder(core, registry);
    }

    public static class Builder {
        private final Core core;
        private final MetricRegistry registry;
        private final Clock clock;
        private final TimeUnit rateUnit;
        private final TimeUnit durationUnit;
        private final MetricFilter filter;

        private Builder(Core core, MetricRegistry registry) {
            this.core = core;
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        public MongoDbMetricsReporter build() {
            return new MongoDbMetricsReporter(
                    core,
                    registry,
                    clock,
                    rateUnit,
                    durationUnit,
                    filter);
        }
    }

    @Override
    public void report(
            SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {
        final Date timestamp = new Date(clock.getTime());

        List<DBObject> docs = Lists.newArrayListWithExpectedSize(
                gauges.size() + counters.size() + histograms.size() + meters.size() +timers.size());

        collectGaugeReports(docs, gauges, timestamp);
        collectCounterReports(docs, counters, timestamp);
        collectHistogramReports(docs, histograms, timestamp);
        collectMeterReports(docs, meters, timestamp);
        collectTimerReports(docs, timers, timestamp);

        try {
            final DBCollection collection = core.getMongoConnection().getDatabase().getCollection("graylog2_metrics");
            // don't hang on to the data for too long.
            final BasicDBObject indexField = new BasicDBObject("timestamp", 1);
            final BasicDBObject indexOptions = new BasicDBObject("expireAfterSeconds", 5 * 60);
            collection.ensureIndex(indexField, indexOptions);

            collection.insert(docs, WriteConcern.UNACKNOWLEDGED);
        } catch (Exception e) {
            log.warn("Unable to write graylog2 metrics to mongodb. Ignoring this error.", e);
        }
    }

    private void collectGaugeReports(List<DBObject> docs, SortedMap<String, Gauge> gauges, Date timestamp) {
        if (gauges.isEmpty())
            return;

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            final BasicDBObject report = getBasicDBObject(timestamp, entry.getKey(), "gauge");
            report.put("value", entry.getValue().getValue());
            docs.add(report);
        }
    }

    private void collectCounterReports(List<DBObject> docs, SortedMap<String, Counter> counters, Date timestamp) {
        if (counters.isEmpty())
            return;

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            final BasicDBObject report = getBasicDBObject(timestamp, entry.getKey(), "counter");
            report.put("count", entry.getValue().getCount());
            docs.add(report);
        }
    }

    private void collectHistogramReports(List<DBObject> docs, SortedMap<String,Histogram> histograms, Date timestamp) {
        if (histograms.isEmpty())
            return;

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            final BasicDBObject report = getBasicDBObject(timestamp, entry.getKey(), "histogram");
            final Histogram histogram = entry.getValue();

            final Snapshot s = histogram.getSnapshot();
            report.put("count", s.size());
            report.put("75th_percentile", s.get75thPercentile());
            report.put("95th_percentile", s.get95thPercentile());
            report.put("98th_percentile", s.get98thPercentile());
            report.put("99th_percentile", s.get99thPercentile());
            report.put("999th_percentile", s.get999thPercentile());
            report.put("max", s.getMax());
            report.put("min", s.getMin());
            report.put("mean", s.getMean());
            report.put("median", s.getMedian());
            report.put("std_dev", s.getStdDev());
            docs.add(report);
        }
    }

    private void collectMeterReports(List<DBObject> docs, SortedMap<String, Meter> meters, Date timestamp) {
        if (meters.isEmpty())
            return;
        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            final BasicDBObject report = getBasicDBObject(timestamp, entry.getKey(), "meter");
            final Meter v = entry.getValue();
            report.put("count", v.getCount());
            report.put("1-minute-rate", v.getOneMinuteRate());
            report.put("5-minute-rate", v.getFiveMinuteRate());
            report.put("15-minute-rate", v.getFifteenMinuteRate());
            report.put("mean-rate", v.getMeanRate());
            docs.add(report);
        }

    }

    private void collectTimerReports(List<DBObject> docs, SortedMap<String,Timer> timers, Date timestamp) {
        if (timers.isEmpty())
            return;
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            final BasicDBObject report = getBasicDBObject(timestamp, entry.getKey(), "timer");
            final Timer v = entry.getValue();
            final Snapshot s = v.getSnapshot();
            // meter part
            report.put("count", v.getCount());
            report.put("rate-unit", getRateUnit());
            report.put("1-minute-rate", convertRate(v.getOneMinuteRate()));
            report.put("5-minute-rate", convertRate(v.getFiveMinuteRate()));
            report.put("15-minute-rate", convertRate(v.getFifteenMinuteRate()));
            report.put("mean-rate", convertRate(v.getMeanRate()));

            // histogram part
            report.put("duration-unit", getDurationUnit());
            report.put("75-percentile", convertDuration(s.get75thPercentile()));
            report.put("95-percentile", convertDuration(s.get95thPercentile()));
            report.put("98-percentile", convertDuration(s.get98thPercentile()));
            report.put("99-percentile", convertDuration(s.get99thPercentile()));
            report.put("999-percentile", convertDuration(s.get999thPercentile()));
            report.put("max", convertDuration(s.getMax()));
            report.put("min", convertDuration(s.getMin()));
            report.put("mean", convertDuration(s.getMean()));
            report.put("median", convertDuration(s.getMedian()));
            report.put("stddev", convertDuration(s.getStdDev()));
            docs.add(report);
        }
    }



    private BasicDBObject getBasicDBObject(Date timestamp, String metricName, String metricType) {
        final BasicDBObject report = new BasicDBObject();

        report.put("_id", new ObjectId());
        report.put("type", metricType);
        report.put("timestamp", timestamp);
        report.put("name", metricName);
        report.put("node", nodeId);

        return report;
    }
}
