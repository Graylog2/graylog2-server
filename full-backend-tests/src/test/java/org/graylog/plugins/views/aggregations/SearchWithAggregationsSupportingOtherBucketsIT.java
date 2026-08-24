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
        api.backend().importElasticsearchFixture("messages-for-other-aggregation-column-truncation-check.json", SearchWithAggregationsSupportingOtherBucketsIT.class);
    }

    private ValidatableResponse execute(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:" + FIXTURE_TYPE_FIELD_VALUE)
                .body(".total", equalTo(10));
    }

    private ValidatableResponse executeEmptyValues(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:551200")
                .body(".total", equalTo(8));
    }

    private ValidatableResponse executeColumnTruncation(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:551201")
                .body(".total", equalTo(17));
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

    @FullBackendTest
    void otherRowCarriesPerColumnValues() {
        // Rows: name (limit 1 -> aaa shown, rest in Other). Columns: age.
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build())
                .columnGroups(Values.builder().field("age").limit(10).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        response.body(".rows[1].key", contains(OTHER_BUCKET_NAME));
        // Tail is bbb (10,20,30), ccc (10,20), ddd (10). By age: 10 -> 3 docs, 20 -> 2 docs, 30 -> 1 doc.
        response.body(".rows[1].values.find{ it.key == ['10', 'count()'] }.value", equalTo(3));
        response.body(".rows[1].values.find{ it.key == ['20', 'count()'] }.value", equalTo(2));
        response.body(".rows[1].values.find{ it.key == ['30', 'count()'] }.value", equalTo(1));
        // age 40 belongs entirely to aaa, so the tail has no cell for it.
        response.body(".rows[1].values.find{ it.key == ['40', 'count()'] }", nullValue());

        // Every doc within a given age column has exactly that age, so avg(age) per column is trivially the
        // column's own value - but only if the extended_stats companion needed to derive it is actually there.
        response.body(".rows[1].values.find{ it.key == ['10', 'avg(age)'] }.value", equalTo(10.0f));
        response.body(".rows[1].values.find{ it.key == ['20', 'avg(age)'] }.value", equalTo(20.0f));
        response.body(".rows[1].values.find{ it.key == ['30', 'avg(age)'] }.value", equalTo(30.0f));
    }

    @FullBackendTest
    void otherRowCarriesPerColumnValuesWithNonInnermostRowGrouping() {
        // Rows: name (limit 1, Other on) then source. "source" is "fbi.org" for every document, so it exists
        // purely to push the opted-in grouping away from the innermost row level. Columns: age.
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build(),
                        Values.builder().field("source").limit(10).build())
                .columnGroups(Values.builder().field("age").limit(10).build())
                .build();
        final ValidatableResponse response = execute(pivot);

        final String otherKey = ".rows.find{ it.key == ['" + OTHER_BUCKET_NAME + "', '" + OTHER_BUCKET_NAME + "'] }";
        response.body(otherKey, notNullValue());
        // Tail is bbb (10,20,30), ccc (10,20), ddd (10) - same tail as otherRowCarriesPerColumnValues. Reading the
        // second column tree off the wrong node (a deeper row grouping's own tree, misread as columns) would
        // report the full column total instead: 4, 3 and 2 respectively.
        response.body(otherKey + ".values.find{ it.key == ['10', 'count()'] }.value", equalTo(3));
        response.body(otherKey + ".values.find{ it.key == ['20', 'count()'] }.value", equalTo(2));
        response.body(otherKey + ".values.find{ it.key == ['30', 'count()'] }.value", equalTo(1));
        response.body(otherKey + ".values.find{ it.key == ['40', 'count()'] }", nullValue());
    }

    @FullBackendTest
    void otherRowColumnCellOmittedWhenSiblingColumnTermsAreTruncated() {
        // Rows: name (limit 1 -> aaa shown, 8 docs). Columns: age (limit 2).
        // Global age totals: 10->6, 20->5, 30->3, 40->3, so the parent's top-2 columns are 10 and 20.
        // aaa's OWN age totals: 30->3, 40->3, 10->1, 20->1, so aaa's own top-2 (limit 2) are 30 and 40 - its
        // report has no bucket for 10 or 20 at all, and its sum_other_doc_count for that sub-aggregation is
        // exactly the 2 documents (age 10 and 20) hidden by its own truncation.
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Sum.builder().field("age").build())
                .rowGroups(Values.builder().field("name").limit(1).otherBucket(true).build())
                .columnGroups(Values.builder().field("age").limit(2).build())
                .build();
        final ValidatableResponse response = executeColumnTruncation(pivot);

        response.body(".rows[1].key", contains(OTHER_BUCKET_NAME));
        // Tail (bbb + ccc) doc count is 9, unaffected by the column-level fix.
        response.body(".rows[1].values.find{ it.key == ['count()'] }.value", equalTo(9));

        // Column 10 and column 20 both fall in aaa's own column-level tail: aaa's true contribution to them (1
        // document each) cannot be read from the response, so both cells must be omitted rather than computed as
        // parentTotal - 0, which would silently report 6 and 5 (and 60.0/100.0 for sum(age)) instead.
        response.body(".rows[1].values.find{ it.key == ['10', 'count()'] }", nullValue());
        response.body(".rows[1].values.find{ it.key == ['10', 'sum(age)'] }", nullValue());
        response.body(".rows[1].values.find{ it.key == ['20', 'count()'] }", nullValue());
        response.body(".rows[1].values.find{ it.key == ['20', 'sum(age)'] }", nullValue());
    }
}
