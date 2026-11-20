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
import org.graylog.testing.mongodb.MongoDBFixtures;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the aggregator against a simplified copy of the Security app's data, loaded from {@code security-events.json}:
 * <pre>
 *     status          risk  tags             timestamp (UTC)  owner      assets             steps taken
 * e1  NEW             12.5  malware, email   Mon 03-04 09:00  ann        web-01             triage, contain
 * e2  NEW             25    malware          Mon 03-04 23:30  ann        web-01, db-01      triage
 * e3  INVESTIGATING   55    phishing, email  Tue 03-05 10:00  ben        laptop-07          (empty)
 * e4  INVESTIGATING   40    (empty)          Wed 03-06 12:00  cid        web-01, laptop-07  -
 * e5  CLOSED          85    malware          Sun 03-10 23:30  ben        printer-02         notify
 * e6  CLOSED          10    (empty)          Mon 03-11 08:00  cid        (empty)            -
 * e7  CLOSED          72    phishing         Mon 03-11 23:15  (deleted)  (deleted)          -
 * e8  FALSE_POSITIVE  -     recon            Tue 03-12 06:00  -          (empty)            -
 *
 * users     account_status    assets      category
 * ann       enabled           web-01      server
 * ben       disabled          db-01       server, database
 * cid       enabled           laptop-07   workstation
 *                             printer-02  (empty)
 * </pre>
 * The event columns are the fields {@code status}, {@code risk_score}, {@code event.tags}, {@code event.timestamp},
 * {@code owner}, {@code event.associated_assets} and {@code actions_taken.step_id} of {@code security_events}. Like in
 * the Security app, events reference their owner and assets by ObjectId, stored as a string; the table shows names
 * instead. Berlin is one hour ahead of UTC in these dates. In {@code event.source_streams}, e7 and e8 are in stream s2,
 * the others in s1.
 */
@ExtendWith(MongoDBExtension.class)
@MongoDBFixtures("security-events.json")
class MongoAggregatorScenariosTest {
    private static final Document ALL = new Document();
    private static final String ANN = "65f000000000000000000001";
    private static final String BEN = "65f000000000000000000002";
    private static final String CID = "65f000000000000000000003";

    private MongoAggregator securityEvents;

    @BeforeEach
    void setUp(final MongoCollections mongoCollections) {
        securityEvents = mongoCollections.aggregator("security_events");
    }

    @Nested
    class GroupingByField {
        @Test
        void countsEventsPerStatus() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms("status")));

            assertThat(counts(result)).containsExactly(
                    entry("CLOSED", 3), entry("INVESTIGATING", 2), entry("NEW", 2), entry("FALSE_POSITIVE", 1));
            assertThat(result.missingCount()).isZero();
        }

        @Test
        void countsEventsPerTag() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms("event.tags")));

            assertThat(counts(result)).containsExactly(
                    entry("malware", 3), entry("email", 2), entry("phishing", 2), entry("recon", 1));
            // e4 and e6 have no tags
            assertThat(result.missingCount()).isEqualTo(2);
        }

        @Test
        void countsEventsPerProcedureStepTaken() {
            final var grouping = Grouping.terms("actions_taken.step_id");

            final var result = securityEvents.buckets(BucketAggregation.of(ALL, grouping));

            assertThat(counts(result)).containsExactly(entry("triage", 2), entry("contain", 1), entry("notify", 1));
            assertThat(result.missingCount()).isEqualTo(5);
        }

        @Test
        void countsOnlyEventsMatchingTheFilter() {
            final var openEvents = Filters.in("status", "NEW", "INVESTIGATING");

            final var result = securityEvents.buckets(BucketAggregation.of(openEvents, Grouping.terms("owner")));

            assertThat(counts(result)).containsExactly(entry(ANN, 2), entry(BEN, 1), entry(CID, 1));
            assertThat(result.missingCount()).isZero();
        }
    }

    @Nested
    class Sorting {
        @Test
        void ordersByCountDescendingByDefault() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("status"));

            // equal counts are ordered by key
            assertThat(keys(securityEvents.buckets(request)))
                    .containsExactly("CLOSED", "INVESTIGATING", "NEW", "FALSE_POSITIVE");
        }

        @Test
        void ordersByCountAscending() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("status"))
                    .withOrder(BucketOrder.byCount(SortOrder.ASCENDING));

            assertThat(keys(securityEvents.buckets(request)))
                    .containsExactly("FALSE_POSITIVE", "INVESTIGATING", "NEW", "CLOSED");
        }

        @Test
        void ordersByKey() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("status"));

            assertThat(keys(securityEvents.buckets(request.withOrder(BucketOrder.byKey(SortOrder.ASCENDING)))))
                    .containsExactly("CLOSED", "FALSE_POSITIVE", "INVESTIGATING", "NEW");
            assertThat(keys(securityEvents.buckets(request.withOrder(BucketOrder.byKey(SortOrder.DESCENDING)))))
                    .containsExactly("NEW", "INVESTIGATING", "FALSE_POSITIVE", "CLOSED");
        }
    }

    @Nested
    class Limit {
        @Test
        void sumsUpBucketsBeyondTheLimit() {
            final var request = BucketAggregation.of(ALL, Grouping.terms("event.tags")).withLimit(2);

            final var result = securityEvents.buckets(request);

            assertThat(counts(result)).containsExactly(entry("malware", 3), entry("email", 2));
            assertThat(result.isTruncated()).isTrue();
            // "phishing" with 2 events and "recon" with 1
            assertThat(result.otherBucketCount()).isEqualTo(2);
            assertThat(result.otherCount()).isEqualTo(3);
            // the number of distinct tags
            assertThat(result.bucketCount()).isEqualTo(4);
            assertThat(result.missingCount()).isEqualTo(2);
        }

        @Test
        void keepsBucketsInTheRequestedOrder() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms("status"))
                    .withOrder(BucketOrder.byKey(SortOrder.ASCENDING))
                    .withLimit(1));

            assertThat(counts(result)).containsExactly(entry("CLOSED", 3));
            assertThat(result.otherBucketCount()).isEqualTo(3);
            assertThat(result.otherCount()).isEqualTo(5);
        }
    }

    @Nested
    class Ranges {
        @Test
        void countsEventsPerRiskLevel() {
            final var grouping = Grouping.ranges(GroupedValueSource.field("risk_score"), List.of(
                    Range.of("low", null, 30),
                    Range.of("medium", 30, 70),
                    Range.of("high", 70, null)));

            final var result = securityEvents.buckets(BucketAggregation.of(ALL, grouping));

            assertThat(counts(result)).containsExactly(entry("low", 3), entry("high", 2), entry("medium", 2));
            // e8 has no risk score yet
            assertThat(result.missingCount()).isEqualTo(1);
        }
    }

    @Nested
    class DateHistogram {
        private static final GroupedValueSource TIMESTAMP = GroupedValueSource.field("event.timestamp");

        @Test
        void countsEventsPerDay() {
            final var result = securityEvents.buckets(inKeyOrder(
                    Grouping.dateHistogram(TIMESTAMP, DateUnit.DAY, ZoneOffset.UTC)));

            assertThat(counts(result)).containsExactly(
                    entry(Instant.parse("2024-03-04T00:00:00Z"), 2),
                    entry(Instant.parse("2024-03-05T00:00:00Z"), 1),
                    entry(Instant.parse("2024-03-06T00:00:00Z"), 1),
                    entry(Instant.parse("2024-03-10T00:00:00Z"), 1),
                    entry(Instant.parse("2024-03-11T00:00:00Z"), 2),
                    entry(Instant.parse("2024-03-12T00:00:00Z"), 1));
        }

        @Test
        void countsEventsPerWeekStartingOnMondayInTimezone() {
            final var utc = securityEvents.buckets(inKeyOrder(
                    Grouping.dateHistogram(TIMESTAMP, DateUnit.WEEK, ZoneOffset.UTC)));
            final var berlin = securityEvents.buckets(inKeyOrder(
                    Grouping.dateHistogram(TIMESTAMP, DateUnit.WEEK, ZoneId.of("Europe/Berlin"))));

            assertThat(counts(utc)).containsExactly(
                    entry(Instant.parse("2024-03-04T00:00:00Z"), 5),
                    entry(Instant.parse("2024-03-11T00:00:00Z"), 3));
            // e5 happened on Sunday night in UTC, but on Monday in Berlin
            assertThat(counts(berlin)).containsExactly(
                    entry(Instant.parse("2024-03-03T23:00:00Z"), 4),
                    entry(Instant.parse("2024-03-10T23:00:00Z"), 4));
        }
    }

    @Nested
    class Relations {
        private static final GroupedValueSource.Related OWNER_ACCOUNT_STATUS =
                GroupedValueSource.related("owner", "users", "_id", "account_status").withLocalFieldAsObjectId();
        private static final GroupedValueSource.Related ASSET_CATEGORY =
                GroupedValueSource.related("event.associated_assets", "assets", "_id", "category")
                        .withLocalFieldAsObjectId();

        @Test
        void countsEventsPerAccountStatusOfTheirOwner() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms(OWNER_ACCOUNT_STATUS)));

            assertThat(counts(result)).containsExactly(entry("enabled", 4), entry("disabled", 2));
            // e7's owner was deleted, e8 has none
            assertThat(result.missingCount()).isEqualTo(2);
        }

        @Test
        void considersOnlyOwnersTheUserMaySee() {
            final var visibleOwners = Filters.in("_id", new ObjectId(ANN), new ObjectId(BEN));
            final var grouping = Grouping.terms(OWNER_ACCOUNT_STATUS.withFilter(visibleOwners));

            final var result = securityEvents.buckets(BucketAggregation.of(ALL, grouping));

            // cid's events e4 and e6 count as missing, like those without a known owner
            assertThat(counts(result)).containsExactly(entry("disabled", 2), entry("enabled", 2));
            assertThat(result.missingCount()).isEqualTo(4);
        }

        @Test
        void countsEventsPerCategoryOfTheirAssets() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms(ASSET_CATEGORY)));

            // e2's two servers count once; e4 has a server and a workstation and counts for both
            assertThat(counts(result))
                    .containsExactly(entry("server", 3), entry("workstation", 2), entry("database", 1));
            // printer-02 has no category, e6 and e8 have no assets, e7's asset was deleted
            assertThat(result.missingCount()).isEqualTo(4);
        }

        @Test
        void sortsAndLimitsRelatedValuesLikeFieldValues() {
            final var result = securityEvents.buckets(BucketAggregation.of(ALL, Grouping.terms(ASSET_CATEGORY))
                    .withOrder(BucketOrder.byKey(SortOrder.ASCENDING))
                    .withLimit(1));

            assertThat(counts(result)).containsExactly(entry("database", 1));
            assertThat(result.otherBucketCount()).isEqualTo(2);
            assertThat(result.otherCount()).isEqualTo(5);
        }
    }

    private static BucketAggregation inKeyOrder(final Grouping grouping) {
        return BucketAggregation.of(ALL, grouping).withOrder(BucketOrder.byKey(SortOrder.ASCENDING));
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
}
