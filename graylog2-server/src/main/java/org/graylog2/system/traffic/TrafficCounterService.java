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
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.internal.update.SingleUpdateOperationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TrafficCounterService {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterService.class);
    private static final String BUCKET = "bucket";

    private final JacksonDBCollection<TrafficDto, ObjectId> db;

    @Inject
    public TrafficCounterService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("traffic"),
                TrafficDto.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject(BUCKET, 1), new BasicDBObject("unique", true));
    }

    private static DateTime getDayBucketStart(DateTime observationTime) {
        return observationTime.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    private static DateTime getHourBucketStart(DateTime observationTime) {
        return observationTime.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    public void updateTraffic(DateTime observationTime, NodeId nodeId, long inLastMinute, long outLastMinute, long decodedLastMinute) {
        // we bucket traffic data by the hour and aggregate it to a day bucket for reporting
        final DateTime dayBucket = getHourBucketStart(observationTime);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating traffic for node {} at {}:  in/decoded/out {}/{}/{} bytes",
                    nodeId, dayBucket, inLastMinute, decodedLastMinute, outLastMinute);
        }

        final String escapedNodeId = nodeId.toEscapedString();
        final WriteResult<TrafficDto, ObjectId> update = db.update(DBQuery.is(BUCKET, dayBucket),
                // sigh DBUpdate.inc only takes integers, but we have a long.
                new DBUpdate.Builder()
                        .addOperation("$inc", "input." + escapedNodeId,
                                new SingleUpdateOperationValue(false, false, inLastMinute))
                        .addOperation("$inc", "output." + escapedNodeId,
                                new SingleUpdateOperationValue(false, false, outLastMinute))
                        .addOperation("$inc", "decoded." + escapedNodeId,
                                new SingleUpdateOperationValue(false, false, decodedLastMinute)),
                true, false);
        if (update.getN() == 0) {
            LOG.warn("Unable to update traffic of node {}: {}", nodeId, update);
        }
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
        final ImmutableMap.Builder<DateTime, Long> inputBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<DateTime, Long> outputBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<DateTime, Long> decodedBuilder = ImmutableMap.builder();

        final DateTime now = Tools.nowUTC();

        // Include traffic up until the current timestamp if includeToday is true.
        // Otherwise, default to the end of the previous day.
        final DateTime to = includeToday ? now : getDayBucketStart(now).minusMillis(1);
        // Make sure to include the full first day
        final DateTime from = getDayBucketStart(now).minus(daysToIncludeDuration);

        final DBQuery.Query query = DBQuery.and(
                DBQuery.lessThanEquals(BUCKET, to),
                DBQuery.greaterThanEquals(BUCKET, from)
        );

        try (DBCursor<TrafficDto> cursor = db.find(query)) {
            cursor.forEach(trafficDto -> {
                inputBuilder.put(trafficDto.bucket(), trafficDto.input().values().stream().mapToLong(Long::valueOf).sum());
                outputBuilder.put(trafficDto.bucket(), trafficDto.output().values().stream().mapToLong(Long::valueOf).sum());
                decodedBuilder.put(trafficDto.bucket(), trafficDto.decoded().values().stream().mapToLong(Long::valueOf).sum());
            });
            Map<DateTime, Long> inputHistogram = inputBuilder.build();
            Map<DateTime, Long> outputHistogram = outputBuilder.build();
            Map<DateTime, Long> decodedHistogram = decodedBuilder.build();

            // we might need to aggregate the hourly database values to their UTC daily buckets
            if (interval == Interval.DAILY) {
                inputHistogram = aggregateToDaily(inputHistogram);
                outputHistogram = aggregateToDaily(outputHistogram);
                decodedHistogram = aggregateToDaily(decodedHistogram);
            }
            return TrafficHistogram.create(from, to, inputHistogram, outputHistogram, decodedHistogram);
        } catch (Exception e) {
            // TODO: remove this diagnostic logging after fixing https://github.com/Graylog2/graylog2-server/issues/9559
            LOG.error("Unable to load traffic data range {} to {}", from, to);
            throw e;
        }
    }

    private TreeMap<DateTime, Long> aggregateToDaily(Map<DateTime, Long> histogram) {
        return histogram.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().withTimeAtStartOfDay(),
                        TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.summingLong(Long::valueOf))));
    }

    public enum Interval {
        HOURLY, DAILY
    }

    @AutoValue
    @JsonAutoDetect
    public abstract static class TrafficHistogram {
        @JsonCreator
        public static TrafficHistogram create(@JsonProperty("from") DateTime from,
                                              @JsonProperty("to") DateTime to,
                                              @JsonProperty("input") Map<DateTime, Long> input,
                                              @JsonProperty("output") Map<DateTime, Long> output,
                                              @JsonProperty("decoded") Map<DateTime, Long> decoded) {
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
