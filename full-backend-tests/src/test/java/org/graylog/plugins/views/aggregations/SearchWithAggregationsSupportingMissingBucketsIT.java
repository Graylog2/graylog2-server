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
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.graylog.plugins.views.search.aggregations.MissingBucketConstants.MISSING_BUCKET_NAME;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

@ContainerMatrixTestsConfiguration(searchVersions = {OS1, ES7, OS2, OS2_LATEST})
public class SearchWithAggregationsSupportingMissingBucketsIT {

    @SuppressWarnings("unused")
    //use this fixtureType:474877 in all fixtures to assure this test isolation from the others
    private static final String FIXTURE_TYPE_FIELD_VALUE = "474877";

    private final GraylogApis api;

    public SearchWithAggregationsSupportingMissingBucketsIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    public void setUp() {
        this.api.backend().importElasticsearchFixture("messages-for-missing-aggregation-check.json", SearchWithAggregationsSupportingMissingBucketsIT.class);
    }

    private ValidatableResponse execute(Pivot pivot) {
        return api.search().executePivot(pivot, "fixtureType:" + FIXTURE_TYPE_FIELD_VALUE)
                .body(".total", equalTo(5));
    }

    @ContainerMatrixTest
    void testSingleFieldAggregationHasProperMissingBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(Values.builder().field("firstName").limit(8).build())
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(".rows", hasSize(5))
                .body(".total", equalTo(5))
                .body(".rows.find{ it.key[0] == 'Joe' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Jane' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Bob' }", notNullValue())
                .body(".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", notNullValue())
                .body(".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket verification (should precede the last/total one - index 3)
        //The only message with "empty" first name in a fixture is {(...)"lastName": "Cooper","age": 60(...)}
        validatableResponse.body(".rows[3].key", contains(MISSING_BUCKET_NAME));
        validatableResponse.body(".rows[3].values[0].key", contains("count()"));
        validatableResponse.body(".rows[3].values[0].value", equalTo(1));
        validatableResponse.body(".rows[3].values[1].key", contains("avg(age)"));
        validatableResponse.body(".rows[3].values[1].value", equalTo(60.0f));

        //Top bucket verification
        //There are 2 "Joes" in a fixture: {(...)"lastName": "Smith","age": 50(...)} and {(...)"lastName": "Biden","age": 80(...)}
        validatableResponse.body(".rows[0].key", contains("Joe"));
        validatableResponse.body(".rows[0].values[0].key", contains("count()"));
        validatableResponse.body(".rows[0].values[0].value", equalTo(2));
        validatableResponse.body(".rows[0].values[1].key", contains("avg(age)"));
        validatableResponse.body(".rows[0].values[1].value", equalTo(65.0f));

    }

    @ContainerMatrixTest
    void testSingleFieldAggregationHasNoMissingBucketWhenSkipEmptyValuesIsUsed() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(Values.builder().field("firstName").limit(8).skipEmptyValues().build())
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(".rows", hasSize(4))
                .body(".total", equalTo(5))
                .body(".rows.find{ it.key[0] == 'Joe' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Jane' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Bob' }", notNullValue())
                .body(".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", nullValue())
                .body(".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket verification (should precede the last/total one - index 3)
        //The only message with "empty" first name in a fixture is {(...)"lastName": "Cooper","age": 60(...)}
        validatableResponse.body(".rows[3].key", not(contains(MISSING_BUCKET_NAME)));

        //Top bucket verification
        //There are 2 "Joes" in a fixture: {(...)"lastName": "Smith","age": 50(...)} and {(...)"lastName": "Biden","age": 80(...)}
        validatableResponse.body(".rows[0].key", contains("Joe"));
        validatableResponse.body(".rows[0].values[0].key", contains("count()"));
        validatableResponse.body(".rows[0].values[0].value", equalTo(2));
        validatableResponse.body(".rows[0].values[1].key", contains("avg(age)"));
        validatableResponse.body(".rows[0].values[1].value", equalTo(65.0f));

    }

    @ContainerMatrixTest
    void testTwoTupledFieldAggregationHasProperMissingBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(
                        Values.builder().fields(List.of("firstName", "lastName")).limit(8).build()
                )
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(tupledItemPath(MISSING_BUCKET_NAME, "Cooper"), hasItems(List.of(1, 60.0f)))
                .body(tupledItemPath("Bob", MISSING_BUCKET_NAME), hasItems(List.of(1, 60.0f)))
                .body(tupledItemPath("Joe", "Smith"), hasItems(List.of(1, 50.0f)))
                .body(tupledItemPath("Joe", "Biden"), hasItems(List.of(1, 80.0f)))
                .body(tupledItemPath("Jane", "Smith"), hasItems(List.of(1, 40.0f)))
                .body(".rows.find{ it.key == [] }.values.value", hasItems(5, 58.0f)) //totals
                .body(".rows", hasSize(6))
                .body(".total", equalTo(5));
    }

    private String tupledItemPath(String... keys) {
        var condition = IntStream.range(0, keys.length)
                .mapToObj(idx -> "it.key[" + idx + "] == '" + keys[idx] + "'")
                .collect(Collectors.joining(" && "));

        return ".rows.findAll { " + condition + " }.values.value";
    }

    @ContainerMatrixTest
    void testTwoTupledFieldAggregationHasNoMissingBucketWhenSkipEmptyValuesIsUsed() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(
                        Values.builder().fields(List.of("firstName", "lastName")).limit(8).skipEmptyValues().build()
                )
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(".rows", hasSize(4))
                .body(".rows.findAll{ it.key[0] == 'Joe' }", hasSize(2)) // Joe-Biden, Joe-Smith
                .body(".rows.findAll{ it.key[0] == 'Jane' }", hasSize(1)) // Jane-Smith
                .body(".rows.findAll{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", hasSize(0))
                .body(".rows.find{ it.key == [] }", notNullValue()) //totals
                .body(".total", equalTo(5));

        //Empty buckets verification
        //We have only one entry with missing first name {(...)"lastName": "Cooper","age": 60(...)}, so both empty buckets will have the same values
        validatableResponse.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }.values.value", nullValue());
    }

    @ContainerMatrixTest
    void testTwoNestedFieldAggregationHasProperMissingBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(
                        Values.builder().field("firstName").limit(8).build(),
                        Values.builder().field("lastName").limit(8).build()
                )
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(".rows", hasSize(6))
                .body(".rows.findAll{ it.key[0] == 'Joe' }", hasSize(2)) // Joe-Biden, Joe-Smith
                .body(".rows.findAll{ it.key[0] == 'Jane' }", hasSize(1)) // Jane-Smith
                .body(".rows.findAll{ it.key == ['Bob', '" + MISSING_BUCKET_NAME + "'] }", hasSize(1)) // Bob has no last name
                .body(".rows.findAll{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", hasSize(1))
                .body(".rows.find{ it.key == [] }", notNullValue()) //totals
                .body(".total", equalTo(5));

        //Empty buckets verification
        //We have only one entry with missing first name {(...)"lastName": "Cooper","age": 60(...)}, so both empty buckets will have the same values
        validatableResponse.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "', 'Cooper'] }.values.value", hasItems(1, 60.0f));
    }

    @ContainerMatrixTest
    void testRowAndColumnPivotHasProperMissingBucket() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .series(Count.builder().build(), Average.builder().field("age").build())
                .rowGroups(Values.builder().field("firstName").limit(1).build())
                .columnGroups(Values.builder().field("lastName").limit(1).build())
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse
                .body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }.values.value", hasItems(1, 60.0f));
    }

    @ContainerMatrixTest
    void testMissingBucketIsNotPresentIfItHasZeroValues() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .rowGroups(Values.builder().field("age").limit(8).build())
                .build();
        final ValidatableResponse validatableResponse = execute(pivot);

        //General verification
        validatableResponse
                .body(".rows", hasSize(5))
                .body(".total", equalTo(5))
                .body(".rows.find{ it.key == ['60'] }", notNullValue())
                .body(".rows.find{ it.key == ['40'] }", notNullValue())
                .body(".rows.find{ it.key == ['50'] }", notNullValue())
                .body(".rows.find{ it.key == ['80'] }", notNullValue())
                .body(".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket is not there - in a fixture all the documents have "age" field
        validatableResponse.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }", nullValue());

        //Top bucket verification
        //There are 2 guys of age 60
        validatableResponse.body(".rows[0].key", contains("60"));
        validatableResponse.body(".rows[0].values[0].key", contains("count()"));
        validatableResponse.body(".rows[0].values[0].value", equalTo(2));

    }
}
