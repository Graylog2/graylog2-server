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
package org.graylog2.system.traffic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TrafficCounterService implements TrafficUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterService.class);
    private static final String BUCKET = "bucket";
    private static final String FIELD_DECODED = "decoded";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_INPUT = "input";

    private final MongoCollection<TrafficDto> collection;

    @Inject
    public TrafficCounterService(final MongoCollections mongoCollections) {
        collection = mongoCollections.collection("traffic", TrafficDto.class);
        collection.createIndex(Indexes.ascending(BUCKET), new IndexOptions().unique(true));
    }

    @Override
    public void updateTraffic(DateTime observationTime,
                              NodeId nodeId,
                              long inLastMinute,
                              long outLastMinute,
                              long decodedLastMinute) {
        // we bucket traffic data by the hour and aggregate it to a day bucket for reporting
        final DateTime dayBucket = TrafficUpdater.getHourBucketStart(observationTime);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating traffic for node {} at {}:  in/decoded/out {}/{}/{} bytes",
                    nodeId, dayBucket, inLastMinute, decodedLastMinute, outLastMinute);
        }

        final String escapedNodeId = nodeId.toEscapedString();
        UpdateResult update = collection.updateOne(Filters.eq(BUCKET, dayBucket), Updates.combine(
                incUpdate(FIELD_INPUT, escapedNodeId, inLastMinute),
                incUpdate(FIELD_OUTPUT, escapedNodeId, outLastMinute),
                incUpdate(FIELD_DECODED, escapedNodeId, decodedLastMinute)
        ), new UpdateOptions().upsert(true));

        if (!update.wasAcknowledged()) {
            LOG.warn("Unable to update traffic of node {} with bucket {}", nodeId, dayBucket);
        }
    }

    public static Bson incUpdate(String fieldInput, String escapedNodeId, long inLastMinute) {
        return Updates.inc("%s.%s".formatted(fieldInput, escapedNodeId), inLastMinute);
    }

    /**
     * Method included for backwards compatibility in pre-5.0 Graylog versions.
     * {@see #clusterTrafficOfLastDays(Duration, Interval, boolean)}
     */
    public TrafficHistogram clusterTrafficOfLastDays(Duration daysToIncludeDuration, Interval interval) {
        return clusterTrafficOfLastDays(daysToIncludeDuration, interval, true);
    }

    /**
     * Queries traffic for the specified duration.
     * <BR>
     * The from-date is considered to be the start of the day that the duration intersects with in the past.
     * For example, if a duration of 1.5 days is specified, then traffic starting from the beginning of two days ago
     * will be returned.
     * <BR>
     * The to-date is considered to be the current date/time (now) when {@code includeToday} false, otherwise then the
     * end of the previous day will be used.
     */
    public TrafficHistogram clusterTrafficOfLastDays(Duration daysToIncludeDuration, Interval interval, boolean includeToday) {
        TrafficCounterService.TrafficHistograms trafficHistograms = createTrafficHistograms(daysToIncludeDuration, includeToday);

        collection.find(createQuery(trafficHistograms.getTo(), trafficHistograms.getFrom())).forEach(trafficDto -> {
            trafficHistograms.add(FIELD_INPUT, trafficDto.bucket(), sumTraffic(trafficDto.input()));
            trafficHistograms.add(FIELD_OUTPUT, trafficDto.bucket(), sumTraffic(trafficDto.output()));
            trafficHistograms.add(FIELD_DECODED, trafficDto.bucket(), sumTraffic(trafficDto.decoded()));
        });

        if (interval == TrafficCounterService.Interval.DAILY) {
            trafficHistograms.aggregateToDaily();
        }

        return TrafficHistogram.create(
                trafficHistograms.getFrom(),
                trafficHistograms.getTo(),
                trafficHistograms.getHistogramOrEmpty(FIELD_INPUT),
                trafficHistograms.getHistogramOrEmpty(FIELD_OUTPUT),
                trafficHistograms.getHistogramOrEmpty(FIELD_DECODED)
        );

    }

    public static TrafficHistograms createTrafficHistograms(Duration daysToIncludeDuration, boolean includeToday) {
        final DateTime now = Tools.nowUTC();
        final DateTime to = includeToday ? now : TrafficUpdater.getDayBucketStart(now).minusMillis(1);
        // Make sure to include the full first day
        final DateTime from = TrafficUpdater.getDayBucketStart(now).minus(daysToIncludeDuration);
        return new TrafficHistograms(from, to);
    }

    public static Bson createQuery(DateTime to, DateTime from) {
        return Filters.and(
                Filters.lte(BUCKET, to),
                Filters.gte(BUCKET, from)
        );
    }

    public static long sumTraffic(Map<String, Long> sumTraffic) {
        return sumTraffic.values().stream().mapToLong(Long::valueOf).sum();
    }

    public enum Interval {
        HOURLY, DAILY
    }

    public static class TrafficHistograms {
        private final Map<String, Map<DateTime, Long>> histograms = new HashMap<>();

        private final DateTime from;
        private final DateTime to;

        public TrafficHistograms(DateTime from, DateTime to) {
            this.from = from;
            this.to = to;
        }

        public void add(String name, DateTime bucket, long value) {
            histograms.computeIfAbsent(name, k -> new HashMap<>()).put(bucket, value);
        }

        public Map<DateTime, Long> getHistogramOrEmpty(String name) {
            return histograms.computeIfAbsent(name, k -> new HashMap<>());
        }

        public long sumTraffic(String name) {
            return Optional.ofNullable(histograms.get(name))
                    .map(traffic -> traffic.values().stream().mapToLong(Long::longValue).sum())
                    .orElse(0L);
        }

        public void aggregateToDaily() {
            histograms.forEach((key, value) -> histograms.put(key, TrafficUpdater.aggregateToDaily(value)));
        }

        public DateTime getFrom() {
            return from;
        }

        public DateTime getTo() {
            return to;
        }
    }

    @AutoValue
    @JsonAutoDetect
    public abstract static class TrafficHistogram {
        @JsonCreator
        public static TrafficHistogram create(@JsonProperty("from") DateTime from,
                                              @JsonProperty("to") DateTime to,
                                              @JsonProperty(FIELD_INPUT) Map<DateTime, Long> input,
                                              @JsonProperty(FIELD_OUTPUT) Map<DateTime, Long> output,
                                              @JsonProperty(FIELD_DECODED) Map<DateTime, Long> decoded) {
            return new AutoValue_TrafficCounterService_TrafficHistogram(from, to, input, output, decoded);
        }

        @JsonProperty
        public abstract DateTime from();

        @JsonProperty
        public abstract DateTime to();

        @JsonProperty
        public abstract Map<DateTime, Long> input();

        @JsonProperty
        public abstract Map<DateTime, Long> output();

        @JsonProperty
        public abstract Map<DateTime, Long> decoded();
    }
}
