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
import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeAll;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.graylog.plugins.views.aggregations.AggregationTestHelpers.serialize;
import static org.graylog.plugins.views.search.aggregations.MissingBucketConstants.MISSING_BUCKET_NAME;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, searchVersions = {OS1, ES7})
public class SearchWithAggregationsSupportingMissingBucketsIT {

    @SuppressWarnings("unused")
    //use this fixtureType:474877 in all fixtures to assure this test isolation from the others
    private static final String FIXTURE_TYPE_FIELD_VALUE = "474877";
    private static final String QUERY_ID = "q1";
    private static final String SEARCH_TYPE_ID = "st1";
    private static final String PIVOT_RESULTS_PATH = "results." + QUERY_ID + ".search_types." + SEARCH_TYPE_ID;

    private final RequestSpecification requestSpec;
    private final GraylogBackend backend;

    public SearchWithAggregationsSupportingMissingBucketsIT(final GraylogBackend backend, final RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
        this.backend = backend;
    }

    @BeforeAll
    public void setUp() {
        backend.importElasticsearchFixture("messages-for-missing-aggregation-check.json", SearchWithAggregationsSupportingMissingBucketsIT.class);
    }

    private ValidatableResponse execute(Pivot pivot) {
        final Pivot pivotWithId = pivot.toBuilder()
                .id(SEARCH_TYPE_ID)
                .build();

        final SearchDTO search = SearchDTO.builder()
                .queries(QueryDTO.builder()
                        .timerange(RelativeRange.create(0))
                        .id(QUERY_ID)
                        .query(ElasticsearchQueryString.of("fixtureType:" + FIXTURE_TYPE_FIELD_VALUE))
                        .searchTypes(Set.of(pivotWithId))
                        .build())
                .build();

        return given()
                .spec(requestSpec)
                .when()
                .body(serialize(search))
                .post("/views/search/sync")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .body("execution.done", equalTo(true))
                .body("execution.completed_exceptionally", equalTo(false))
                .body(PIVOT_RESULTS_PATH + ".total", equalTo(5));
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
        validatableResponse.rootPath(PIVOT_RESULTS_PATH)
                .body(".rows", hasSize(5))
                .body(".total", equalTo(5))
                .body(".rows.find{ it.key[0] == 'Joe' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Jane' }", notNullValue())
                .body(".rows.find{ it.key[0] == 'Bob' }", notNullValue())
                .body(".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", notNullValue())
                .body(".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket verification (should precede the last/total one - index 3)
        //The only message with "empty" first name in a fixture is {(...)"lastName": "Cooper","age": 60(...)}
        validatableResponse.body(".rows[3].key", contains("(Empty Value)"));
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
    void testTwoFieldAggregationHasProperMissingBucket() {
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
        validatableResponse.rootPath(PIVOT_RESULTS_PATH)
                .body(".rows", hasSize(5))
                .body(".rows.findAll{ it.key[0] == 'Joe' }", hasSize(2)) // Joe-Biden, Joe-Smith
                .body(".rows.findAll{ it.key[0] == 'Jane' }", hasSize(1)) // Jane-Smith
                .body(".rows.findAll{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", hasSize(1))
                .body(".rows.find{ it.key == [] }", notNullValue()) //totals
                .body(".total", equalTo(5));

        //Empty buckets verification
        //We have only one entry with missing first name {(...)"lastName": "Cooper","age": 60(...)}, so both empty buckets will have the same values
        validatableResponse.body(".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "'] }.values.value", hasItems(2, 60.0f));
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
        validatableResponse.body("execution.done", equalTo(true))
                .rootPath(PIVOT_RESULTS_PATH)
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
