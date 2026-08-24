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
package org.graylog.plugins.views.aggregations;

import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.junit.jupiter.api.BeforeAll;

import static org.graylog.plugins.views.search.aggregations.MissingBucketConstants.MISSING_BUCKET_NAME;
import static org.graylog.plugins.views.search.aggregations.OtherBucketConstants.OTHER_BUCKET_NAME;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@GraylogBackendConfiguration
public class SearchWithAggregationsSupportingOtherBucketsIT {

    @SuppressWarnings("unused")
    //use this fixtureType:551199 in all fixtures to assure this test isolation from the others
    private static final String FIXTURE_TYPE_FIELD_VALUE = "551199";

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
        api.backend().importElasticsearchFixture("messages-for-other-aggregation-check.json", SearchWithAggregationsSupportingOtherBucketsIT.class);
        api.backend().importElasticsearchFixture("messages-for-other-aggregation-empty-values-check.json", SearchWithAggregationsSupportingOtherBucketsIT.class);
    }

    private ValidatableResponse execute(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:" + FIXTURE_TYPE_FIELD_VALUE)
                .body(".total", equalTo(10));
    }

    private ValidatableResponse executeEmptyValues(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:551200")
                .body(".total", equalTo(8));
    }

    @FullBackendTest
    void otherBucketCollectsEverythingBeyondTheLimit() {
        // Limit 2 keeps aaa (4 docs) and bbb (3 docs); ccc (2 docs) and ddd (1 doc) fall into (Other).
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Sum.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(2).otherBucket(true).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        // aaa, bbb, (Other), rollup
        response.body(".rows", hasSize(4));
        response.body(".rows[0].key", contains("aaa"));
        response.body(".rows[1].key", contains("bbb"));
        response.body(".rows[2].key", contains(OTHER_BUCKET_NAME));
        response.body(".rows[3].key", empty());

        // ccc: 2 docs, ages 10+20=30. ddd: 1 doc, age 10. Tail: 3 docs, sum 40.
        response.body(".rows[2].values.find{ it.key == ['count()'] }.value", equalTo(3));
        response.body(".rows[2].values.find{ it.key == ['sum(age)'] }.value", equalTo(40.0f));
    }

    @FullBackendTest
    void otherBucketIsAbsentWhenLimitCoversEveryValue() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .rowGroups(Values.builder().field("name").limit(10).otherBucket(true).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        response.body(".rows", hasSize(5));
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "'] }", nullValue());
    }

    @FullBackendTest
    void otherBucketIsAbsentWhenTheFlagIsOff() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .rowGroups(Values.builder().field("name").limit(2).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        response.body(".rows", hasSize(3));
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "'] }", nullValue());
    }

    @FullBackendTest
    void shownBucketsPlusOtherEqualTheRollup() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Sum.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        // aaa: 4 docs, sum 100. Other: 6 docs, sum 100. Rollup: 10 docs, sum 200.
        response.body(".rows[0].values.find{ it.key == ['count()'] }.value", equalTo(4));
        response.body(".rows[0].values.find{ it.key == ['sum(age)'] }.value", equalTo(100.0f));
        response.body(".rows[1].values.find{ it.key == ['count()'] }.value", equalTo(6));
        response.body(".rows[1].values.find{ it.key == ['sum(age)'] }.value", equalTo(100.0f));
        response.body(".rows[2].values.find{ it.key == ['count()'] }.value", equalTo(10));
        response.body(".rows[2].values.find{ it.key == ['sum(age)'] }.value", equalTo(200.0f));
    }

    @FullBackendTest
    void averageIsDerivedExactlyForTheOtherBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Average.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        // Tail is bbb (10,20,30), ccc (10,20), ddd (10): 6 values summing to 100 -> avg 16.666...
        response.body(".rows[1].key", contains(OTHER_BUCKET_NAME));
        response.body(".rows[1].values.find{ it.key == ['avg(age)'] }.value",
                equalTo(100.0f / 6.0f));
    }

    @FullBackendTest
    void nonDerivableSeriesAreOmittedFromTheOtherBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Max.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        response.body(".rows[1].key", contains(OTHER_BUCKET_NAME));
        response.body(".rows[1].values.find{ it.key == ['count()'] }.value", equalTo(6));
        response.body(".rows[1].values.find{ it.key == ['max(age)'] }", nullValue());
    }

    @FullBackendTest
    void otherBucketKeyIsPaddedForDeeperGroupings() {
        // Rows: name (limit 1, Other on) then age. The (Other) row is a leaf, so its key is padded to full arity.
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build(),
                        Values.builder().field("age").limit(10).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "', '" + OTHER_BUCKET_NAME + "'] }",
                notNullValue());
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "', '" + OTHER_BUCKET_NAME + "'] }"
                        + ".values.find{ it.key == ['count()'] }.value",
                equalTo(6));
        // No short key is emitted alongside it.
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "'] }", nullValue());
    }

    @FullBackendTest
    void otherBucketExcludesTheEmptyValueDocuments() {
        // filters[0] ("name exists") holds p/q/r; filters[1] ("name missing") holds the two 1000-age docs.
        // The sets are disjoint, so the (Other) tail is r alone - the (Empty Value) documents must not be
        // folded into it. Folding them in previously produced sum(age) = -1995 instead of 5.
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Sum.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(2).otherBucket(true).build())
                .build();
        final ValidatableResponse response = executeEmptyValues(pivot);

        // (Empty Value) survives as its own row, with its own documents.
        response.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }.values.find{ it.key == ['count()'] }.value",
                equalTo(2));
        response.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }.values.find{ it.key == ['sum(age)'] }.value",
                equalTo(2000.0f));

        // The tail is r alone: 1 document, age 5.
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "'] }.values.find{ it.key == ['count()'] }.value",
                equalTo(1));
        response.body(".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "'] }.values.find{ it.key == ['sum(age)'] }.value",
                equalTo(5.0f));

        // Everything still adds up: p(3) + q(2) + (Empty Value)(2) + (Other)(1) = 8.
        response.body(".rows.find{ it.key == [] }.values.find{ it.key == ['count()'] }.value", equalTo(8));
        response.body(".rows.find{ it.key == [] }.values.find{ it.key == ['sum(age)'] }.value", equalTo(2075.0f));
    }
}
