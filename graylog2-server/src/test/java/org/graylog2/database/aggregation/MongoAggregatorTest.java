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
package org.graylog2.database.aggregation;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.aggregation.BucketAggregationResult.Bucket;
import org.graylog2.database.aggregation.Grouping.DateUnit;
import org.graylog2.database.aggregation.Grouping.Range;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class MongoAggregatorTest {
    private static final Document ALL = new Document();

    private MongoCollections mongoCollections;
    private MongoDBTestService mongodb;
    private MongoAggregator items;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections, final MongoDBTestService mongodb) {
        this.mongoCollections = mongoCollections;
        this.mongodb = mongodb;
        this.items = mongoCollections.aggregator("items");
        mongodb.mongoCollection("items").insertMany(List.of(
                new Document("_id", "i1").append("owner", "alice").append("status", "open").append("score", 10)
                        .append("tags", List.of("a", "b")).append("timestamp", date("2024-01-01T23:30:00Z"))
                        .append("assets", List.of(new Document("id", "x"), new Document("id", "y"))),
                new Document("_id", "i2").append("owner", "alice").append("status", "closed").append("score", 20)
                        .append("tags", List.of("a", "a")).append("timestamp", date("2024-01-02T00:30:00Z"))
                        .append("assets", List.of(new Document("id", "x"))),
                new Document("_id", "i3").append("owner", "bob").append("status", "open").append("score", 70)
                        .append("tags", List.of()).append("timestamp", date("2024-01-02T10:00:00Z")),
                new Document("_id", "i4").append("owner", null).append("status", "open").append("score", "high")
                        .append("tags", List.of("c")).append("timestamp", "not a date"),
                new Document("_id", "i5").append("status", "open").append("score", 95)
                        .append("timestamp", date("2024-01-03T12:00:00Z")),
                new Document("_id", "i6").append("owner", "carol").append("status", "closed").append("score", 50)
                        .append("timestamp", date("2024-01-03T12:00:00Z"))));
    }

    @Nested
    class Terms {
        @Test
        void countsDocumentsPerValueAndCollectsMissingValues() {
            final var result = items.buckets(BucketAggregation.of(ALL, Grouping.terms("owner")));

            // equal counts are ordered by key
            assertThat(counts(result)).containsExactly(entry("alice", 2L), entry("bob", 1L), entry("carol", 1L));
            // null and absent owners
            assertThat(result.missingCount()).isEqualTo(2);
            assertThat(result.isTruncated()).isFalse();
            assertThat(result.bucketCount()).isEqualTo(3);
        }

        @Test
        void countsEachDocumentOncePerDistinctArrayElement() {
            final var result = items.buckets(BucketAggregation.of(ALL, Grouping.terms("tags")));

            assertThat(counts(result)).containsExactly(entry("a", 2L), entry("b", 1L), entry("c", 1L));
            // an empty array counts as missing, like an absent field
            assertThat(result.missingCount()).isEqualTo(3);
        }

        @Test
        void groupsByPathThroughArrayOfEmbeddedDocuments() {
            final var result = items.buckets(BucketAggregation.of(ALL, Grouping.terms("assets.id")));

            assertThat(counts(result)).containsExactly(entry("x", 2L), entry("y", 1L));
            assertThat(result.missingCount()).isEqualTo(4);
        }

        @Test
        void appliesFilterBeforeGrouping() {
            final var result = items.buckets(BucketAggregation.of(Filters.eq("status", "closed"), Grouping.terms("owner")));

            assertThat(counts(result)).containsExactly(entry("alice", 1L), entry("carol", 1L));
            assertThat(result.missingCount()).isZero();
        }

        @Test
        void returnsNoBucketsWhenNothingMatches() {
            final var result = items.buckets(BucketAggregation.of(Filters.eq("owner", "nobody"), Grouping.terms("owner")));

            assertThat(result.buckets()).isEmpty();
            assertThat(result.missingCount()).isZero();
            assertThat(result.isTruncated()).isFalse();
        }
    }

    @Nested
    class LimitAndOrder {
        @Test
        void sumsUpBucketsBeyondLimit() {
            final var result = items.buckets(BucketAggregation.of(ALL, Grouping.terms("owner")).withLimit(1));

            assertThat(counts(result)).containsExactly(entry("alice", 2L));
            assertThat(result.isTruncated()).isTrue();
            assertThat(result.otherBucketCount()).isEqualTo(2);
            assertThat(result.otherCount()).isEqualTo(2);
            assertThat(result.bucketCount()).isEqualTo(3);
            // the missing bucket isn't subject to the limit
            assertThat(result.missingCount()).isEqualTo(2);
        }

        @Test
        void ordersByKey() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("owner"))
                    .withOrder(BucketOrder.byKey(SortOrder.DESCENDING));

            assertThat(keys(items.buckets(request))).containsExactly("carol", "bob", "alice");
        }

        @Test
        void ordersByCountAscending() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("owner"))
                    .withOrder(BucketOrder.byCount(SortOrder.ASCENDING));

            // equal counts are still ordered by key ascending
            assertThat(keys(items.buckets(request))).containsExactly("bob", "carol", "alice");
        }
    }

    @Nested
    class Ranges {
        @Test
        void bucketsNumbersByRangeAndTreatsOtherTypesAsMissing() {
            final var grouping = Grouping.ranges(GroupedValueSource.field("score"), List.of(
                    Range.of("low", null, 30),
                    Range.of("medium", 30, 80),
                    Range.of("high", 80, null)));

            final var result = items.buckets(BucketAggregation.of(ALL, grouping));

            assertThat(counts(result)).containsExactly(entry("low", 2L), entry("medium", 2L), entry("high", 1L));
            // the string "high" isn't a number
            assertThat(result.missingCount()).isEqualTo(1);
        }

        @Test
        void bucketsInstantsByRange() {
            final var grouping = Grouping.ranges(GroupedValueSource.field("timestamp"), List.of(
                    Range.of("before", null, Instant.parse("2024-01-02T00:00:00Z")),
                    Range.of("after", Instant.parse("2024-01-02T00:00:00Z"), null)));

            final var result = items.buckets(BucketAggregation.of(ALL, grouping));

            assertThat(counts(result)).containsExactly(entry("after", 4L), entry("before", 1L));
        }

        @Test
        void treatsRangeKeysAsLiterals() {
            final var grouping = Grouping.ranges(GroupedValueSource.field("score"),
                    List.of(Range.of("$owner", 0, 100)));

            assertThat(keys(items.buckets(BucketAggregation.of(ALL, grouping)))).containsExactly("$owner");
        }
    }

    @Nested
    class DateHistogram {
        @Test
        void bucketsByCalendarDay() {
            final var grouping = Grouping.dateHistogram(GroupedValueSource.field("timestamp"), DateUnit.DAY,
                    ZoneOffset.UTC);

            final var result = items.buckets(BucketAggregation.of(ALL, grouping)
                    .withOrder(BucketOrder.byKey(SortOrder.ASCENDING)));

            assertThat(counts(result)).containsExactly(
                    entry(Instant.parse("2024-01-01T00:00:00Z"), 1L),
                    entry(Instant.parse("2024-01-02T00:00:00Z"), 2L),
                    entry(Instant.parse("2024-01-03T00:00:00Z"), 2L));
            // "not a date"
            assertThat(result.missingCount()).isEqualTo(1);
        }

        @Test
        void alignsDaysToTimezone() {
            final var grouping = Grouping.dateHistogram(GroupedValueSource.field("timestamp"), DateUnit.DAY,
                    ZoneId.of("Europe/Berlin"));

            final var result = items.buckets(BucketAggregation.of(ALL, grouping)
                    .withOrder(BucketOrder.byKey(SortOrder.ASCENDING)));

            // 23:30 UTC is already the next day in Berlin
            assertThat(counts(result)).containsExactly(
                    entry(Instant.parse("2024-01-01T23:00:00Z"), 3L),
                    entry(Instant.parse("2024-01-02T23:00:00Z"), 2L));
        }
    }

    @Nested
    class RelatedValues {
        private final ObjectId red = new ObjectId();
        private final ObjectId blue = new ObjectId();

        @BeforeEach
        void setUp() {
            mongodb.mongoCollection("events").insertMany(List.of(
                    new Document("_id", "e1").append("event_id", "e1").append("owner", red.toHexString()),
                    new Document("_id", "e2").append("event_id", "e2").append("owner", red.toHexString()),
                    new Document("_id", "e3").append("event_id", "e3").append("owner", blue.toHexString()),
                    new Document("_id", "e4").append("event_id", "e4").append("owner", "not an id"),
                    new Document("_id", "e5")));
            mongodb.mongoCollection("investigations").insertMany(List.of(
                    new Document("status", "open").append("team", "red").append("events", List.of("e1", "e2")),
                    new Document("status", "open").append("team", "blue").append("events", List.of("e1")),
                    new Document("status", "closed").append("team", "red").append("events", List.of("e3")),
                    // must not be linked to e5, which has no event_id either
                    new Document("status", "unlinked").append("team", "red")));
            mongodb.mongoCollection("users").insertMany(List.of(
                    new Document("_id", red).append("team", "red"),
                    new Document("_id", blue).append("team", "blue")));
        }

        @Test
        void groupsByFieldOfReferencingDocuments() {
            final var grouping = Grouping.terms(
                    GroupedValueSource.related("event_id", "investigations", "events", "status"));

            final var result = mongoCollections.aggregator("events").buckets(BucketAggregation.of(ALL, grouping));

            // e1 is in two open investigations, but counts once
            assertThat(counts(result)).containsExactly(entry("open", 2L), entry("closed", 1L));
            assertThat(result.missingCount()).isEqualTo(2);
        }

        @Test
        void considersOnlyRelatedDocumentsMatchingTheirFilter() {
            final var grouping = Grouping.terms(
                    GroupedValueSource.related("event_id", "investigations", "events", "status")
                            .withFilter(Filters.eq("team", "blue")));

            final var result = mongoCollections.aggregator("events").buckets(BucketAggregation.of(ALL, grouping));

            assertThat(counts(result)).containsExactly(entry("open", 1L));
            assertThat(result.missingCount()).isEqualTo(4);
        }

        @Test
        void convertsStringReferencesToObjectIds() {
            final var source = GroupedValueSource.related("owner", "users", "_id", "team");
            final var events = mongoCollections.aggregator("events");

            final var converted = events.buckets(BucketAggregation.of(ALL, Grouping.terms(source.withLocalFieldAsObjectId())));
            assertThat(counts(converted)).containsExactly(entry("red", 2L), entry("blue", 1L));
            // the invalid and the absent reference
            assertThat(converted.missingCount()).isEqualTo(2);

            final var unconverted = events.buckets(BucketAggregation.of(ALL, Grouping.terms(source)));
            assertThat(unconverted.buckets()).isEmpty();
        }
    }

    private static Map<Object, Long> counts(final BucketAggregationResult result) {
        final var counts = new LinkedHashMap<Object, Long>();
        result.buckets().forEach(bucket -> counts.put(bucket.key(), bucket.count()));
        return counts;
    }

    private static List<Object> keys(final BucketAggregationResult result) {
        return result.buckets().stream().map(Bucket::key).toList();
    }

    private static Map.Entry<Object, Long> entry(final Object key, final long count) {
        return Map.entry(key, count);
    }

    private static Date date(final String instant) {
        return Date.from(Instant.parse(instant));
    }
}
