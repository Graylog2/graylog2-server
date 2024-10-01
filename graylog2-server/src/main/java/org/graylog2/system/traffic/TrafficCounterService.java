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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import jakarta.inject.Inject;
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

import java.util.Map;
import java.util.Optional;

public class TrafficCounterService implements TrafficUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterService.class);
    private static final String BUCKET = "bucket";
    private static final String FIELD_DATA_WAREHOUSE_OUTPUT = "data_warehouse_output";
    private static final String FIELD_DECODED = "decoded";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_INPUT = "input";

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

    @Override
    public void updateTraffic(DateTime observationTime,
                              NodeId nodeId,
                              long inLastMinute,
                              long outLastMinute,
                              long decodedLastMinute,
                              long dataWarehouseOutLastMinute) {
        // we bucket traffic data by the hour and aggregate it to a day bucket for reporting
        final DateTime dayBucket = TrafficUpdater.getHourBucketStart(observationTime);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating traffic for node {} at {}:  in/decoded/out {}/{}/{} bytes",
                    nodeId, dayBucket, inLastMinute, decodedLastMinute, outLastMinute);
        }

        final String escapedNodeId = nodeId.toEscapedString();
        final WriteResult<TrafficDto, ObjectId> update = db.update(DBQuery.is(BUCKET, dayBucket),
                // sigh DBUpdate.inc only takes integers, but we have a long.
                new DBUpdate.Builder()
                        .addOperation("$inc", "%s.%s".formatted(FIELD_INPUT, escapedNodeId),
                                new SingleUpdateOperationValue(false, false, inLastMinute))
                        .addOperation("$inc", "%s.%s".formatted(FIELD_OUTPUT, escapedNodeId),
                                new SingleUpdateOperationValue(false, false, outLastMinute))
                        .addOperation("$inc", "%s.%s".formatted(FIELD_DECODED, escapedNodeId),
                                new SingleUpdateOperationValue(false, false, decodedLastMinute))
                        .addOperation("$inc", "%s.%s".formatted(FIELD_DATA_WAREHOUSE_OUTPUT, escapedNodeId),
                                new SingleUpdateOperationValue(false, false, dataWarehouseOutLastMinute)),
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
        final ImmutableMap.Builder<DateTime, Long> dataWarehouseOutputBuilder = ImmutableMap.builder();

        final DateTime now = Tools.nowUTC();

        // Include traffic up until the current timestamp if includeToday is true.
        // Otherwise, default to the end of the previous day.
        final DateTime to = includeToday ? now : TrafficUpdater.getDayBucketStart(now).minusMillis(1);
        // Make sure to include the full first day
        final DateTime from = TrafficUpdater.getDayBucketStart(now).minus(daysToIncludeDuration);

        final DBQuery.Query query = DBQuery.and(
                DBQuery.lessThanEquals(BUCKET, to),
                DBQuery.greaterThanEquals(BUCKET, from)
        );

        try (DBCursor<TrafficDto> cursor = db.find(query)) {
            cursor.forEach(trafficDto -> {
                inputBuilder.put(trafficDto.bucket(), trafficDto.input().values().stream().mapToLong(Long::valueOf).sum());
                outputBuilder.put(trafficDto.bucket(), trafficDto.output().values().stream().mapToLong(Long::valueOf).sum());
                decodedBuilder.put(trafficDto.bucket(), trafficDto.decoded().values().stream().mapToLong(Long::valueOf).sum());
                dataWarehouseOutputBuilder.put(trafficDto.bucket(), Optional.ofNullable(trafficDto.dataWarehouseOutput())
                        .map(stringLongMap -> stringLongMap.values().stream().mapToLong(Long::valueOf).sum())
                        .orElse(0L));
            });
            Map<DateTime, Long> inputHistogram = inputBuilder.build();
            Map<DateTime, Long> outputHistogram = outputBuilder.build();
            Map<DateTime, Long> decodedHistogram = decodedBuilder.build();
            Map<DateTime, Long> dataWarehouseOutputHistogram = dataWarehouseOutputBuilder.build();

            // we might need to aggregate the hourly database values to their UTC daily buckets
            if (interval == Interval.DAILY) {
                inputHistogram = TrafficUpdater.aggregateToDaily(inputHistogram);
                outputHistogram = TrafficUpdater.aggregateToDaily(outputHistogram);
                decodedHistogram = TrafficUpdater.aggregateToDaily(decodedHistogram);
                dataWarehouseOutputHistogram = TrafficUpdater.aggregateToDaily(dataWarehouseOutputHistogram);
            }
            return TrafficHistogram.create(from, to, inputHistogram, outputHistogram, decodedHistogram, dataWarehouseOutputHistogram);
        }
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
                                              @JsonProperty(FIELD_INPUT) Map<DateTime, Long> input,
                                              @JsonProperty(FIELD_OUTPUT) Map<DateTime, Long> output,
                                              @JsonProperty(FIELD_DECODED) Map<DateTime, Long> decoded,
                                              @JsonProperty(FIELD_DATA_WAREHOUSE_OUTPUT) Map<DateTime, Long> dataWarehouseOutput) {
            return new AutoValue_TrafficCounterService_TrafficHistogram(from, to, input, output, decoded, dataWarehouseOutput);
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

        @JsonIgnore
        public abstract Map<DateTime, Long> dataWarehouseOutput();
    }
}
