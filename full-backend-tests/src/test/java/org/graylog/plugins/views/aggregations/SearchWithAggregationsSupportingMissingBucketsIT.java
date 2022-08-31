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
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.graylog.plugins.views.search.aggregations.MissingBucketConstants.MISSING_BUCKET_NAME;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, searchVersions = {OS1, ES7})
public class SearchWithAggregationsSupportingMissingBucketsIT {

    @SuppressWarnings("unused")
    //use this fixtureType:474877 in all fixtures to assure this test isolation from the others
    private static final String FIXTURE_TYPE_FIELD_VALUE = "474877";
    private static final String EXEC_PATH = "/views/search/sync";
    private static final String QUERY_ID = "q1";
    private static final String SEARCH_TYPE_ID = "st1";


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

    @ContainerMatrixTest
    void testSingleFieldAggregationHasProperMissingBucket() {
        final String pivotSearchTypePathInResponse = "results." + QUERY_ID + ".search_types." + SEARCH_TYPE_ID;
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/aggregations/search_request_with_aggregation_on_single_field.json")) //aggregates on the 'firstName' field
                .post(EXEC_PATH)
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
        //General verification
        validatableResponse.body("execution.done", equalTo(true))
                .body(pivotSearchTypePathInResponse + ".rows", hasSize(5))
                .body(pivotSearchTypePathInResponse + ".total", equalTo(5))
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == 'Joe' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == 'Jane' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == 'Bob' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket verification (should precede the last/total one - index 3)
        //The only message with "empty" first name in a fixture is {(...)"lastName": "Cooper","age": 60(...)}
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[3].key", contains("(Empty Value)"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[3].values[0].key", contains("count()"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[3].values[0].value", equalTo(1));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[3].values[1].key", contains("avg(age)"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[3].values[1].value", equalTo(60.0f));

        //Top bucket verification
        //There are 2 "Joes" in a fixture: {(...)"lastName": "Smith","age": 50(...)} and {(...)"lastName": "Biden","age": 80(...)}
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].key", contains("Joe"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[0].key", contains("count()"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[0].value", equalTo(2));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[1].key", contains("avg(age)"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[1].value", equalTo(65.0f));

    }

    @ContainerMatrixTest
    void testTwoFieldAggregationHasProperMissingBucket() {
        final String pivotSearchTypePathInResponse = "results." + QUERY_ID + ".search_types." + SEARCH_TYPE_ID;
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/aggregations/search_request_with_aggregation_on_two_fields.json")) //aggregates on the 'firstName' and 'lastName' fields
                .post(EXEC_PATH)
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
        //General verification
        validatableResponse.body("execution.done", equalTo(true))
                .body(pivotSearchTypePathInResponse + ".rows", hasSize(10))
                .body(pivotSearchTypePathInResponse + ".rows.findAll{ it.key[0] == 'Joe' }", hasSize(3)) //Joe, Joe-Biden, Joe-Smith
                .body(pivotSearchTypePathInResponse + ".rows.findAll{ it.key[0] == 'Bob' }", hasSize(2)) //Bob, Bob-empty
                .body(pivotSearchTypePathInResponse + ".rows.findAll{ it.key[0] == 'Jane' }", hasSize(2)) //Jane, Jane-Smith
                .body(pivotSearchTypePathInResponse + ".rows.findAll{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", hasSize(2)) //empty, empty-Cooper
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key == [] }", notNullValue()) //totals
                .body(pivotSearchTypePathInResponse + ".total", equalTo(5));

        //Empty buckets verification
        //We have only one entry with missing first name {(...)"lastName": "Cooper","age": 60(...)}, so both empty buckets will have the same values
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' && it.key[1] == 'Cooper'}.values[0].value", equalTo(1))
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' && it.key[1] == 'Cooper'}.values[1].value", equalTo(60.0f))
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "']}.values[0].value", equalTo(1))
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key == ['" + MISSING_BUCKET_NAME + "']}.values[1].value", equalTo(60.0f));


    }

    @ContainerMatrixTest
    void testMissingBucketIsNotPresentIfItHasZeroValues() {
        final String pivotSearchTypePathInResponse = "results." + QUERY_ID + ".search_types." + SEARCH_TYPE_ID;
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/aggregations/search_request_with_aggregation_that_has_no_missing_buckets.json")) //aggregates on the 'age' field
                .post(EXEC_PATH)
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        //General verification
        validatableResponse.body("execution.done", equalTo(true))
                .body(pivotSearchTypePathInResponse + ".rows", hasSize(5))
                .body(pivotSearchTypePathInResponse + ".total", equalTo(5))
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '60' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '40' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '50' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '80' }", notNullValue())
                .body(pivotSearchTypePathInResponse + ".rows.find{ it.key == [] }", notNullValue());

        //Empty bucket is not there - in a fixture all the documents have "age" field
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows.find{ it.key[0] == '" + MISSING_BUCKET_NAME + "' }", nullValue());


        //Top bucket verification
        //There are 2 guys of age 60
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].key", contains("60"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[0].key", contains("count()"));
        validatableResponse.body(pivotSearchTypePathInResponse + ".rows[0].values[0].value", equalTo(2));

    }

    private InputStream fixture(final String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

}
