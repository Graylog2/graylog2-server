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
package org.graylog.plugins.views;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS, mongoVersions = MongodbServer.MONGO5, searchVersions = SearchServer.OS1)
public class QueryValidationResourceIT {

    private final RequestSpecification requestSpec;
    private final GraylogApis api;

    public QueryValidationResourceIT(RequestSpecification requestSpec, GraylogApis api) {
        this.requestSpec = requestSpec;
        this.api = api;
    }

    @BeforeAll
    public void importMessage() {
        api.gelf()
                .createGelfHttpInput()
                .postMessage(
                        """
                                {
                                "short_message":"query-validation-test",
                                "host":"example.org",
                                "type":"ssh",
                                "source":"example.org",
                                "http_response_code":200,
                                "bytes":42,
                                "timestamp": "2019-07-23 09:53:08.175",
                                "otherDate": "2020-07-29T12:00:00.000-05:00",
                                "resource": "posts",
                                "always_find_me": "whatever",
                                "level":3
                                }""");

        api.search().waitForMessage("query-validation-test");
        api.fieldTypes().waitForFieldTypeDefinitions("level");
    }


    @ContainerMatrixTest
    void testMinimalisticRequest() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"foo:bar\"}")
                .post("/search/validate")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("WARNING"));
    }

    @ContainerMatrixTest
    void testInvalidQuery() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"foo:\"}")
                .post("/search/validate")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("ERROR"));
        validatableResponse.assertThat().body("explanations.error_message[0]", containsString("Cannot parse query, cause: incomplete query, query ended unexpectedly"));
    }

    @ContainerMatrixTest
    void testOrQuery() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"unknown_field:(x OR y)\"}")
                .post("/search/validate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validatableResponse.assertThat().body("status", equalTo("WARNING"));
        validatableResponse.assertThat().body("explanations.error_message[0]", containsString("Query contains unknown field: unknown_field"));
    }

    @ContainerMatrixTest
    void testRegexWithoutFieldName() {
        verifyQueryIsValidatedSuccessfully("/ethernet[0-9]+/");
    }

    @ContainerMatrixTest
    void testLowercaseNotOperator() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"not(http_response_code:200)\"}")
                .post("/search/validate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("WARNING"));
        validatableResponse.assertThat().body("explanations.error_message[0]", containsString("Query contains invalid operator \"not\". All AND / OR / NOT operators have to be written uppercase"));
    }

    @ContainerMatrixTest
    void testInvalidValueType() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"timestamp:AAA\"}")
                .post("/search/validate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("WARNING"));
        validatableResponse.assertThat().body("explanations.error_type[0]", equalTo("INVALID_VALUE_TYPE"));
    }

    @ContainerMatrixTest
    void testSuccessfullyValidatesExistsTerms() {
        verifyQueryIsValidatedSuccessfully("_exists_:timestamp");
        verifyQueryIsValidatedSuccessfully("_exists_:level");
    }

    @ContainerMatrixTest
    void testQuotedDefaultField() {
        // if the validation correctly recognizes the quoted text, it should not warn about lowercase or
        verifyQueryIsValidatedSuccessfully("\\\"A or B\\\"");
    }


    @ContainerMatrixTest
    void testQueriesFromDocumentationAreValidatedSuccessfully() {
        //Uses https://docs.graylog.org/docs/query-language as a source of documented queries (accessed 27.06.2022)
        verifyQueryIsValidatedSuccessfully("ssh");
        verifyQueryIsValidatedSuccessfully("ssh login");
        verifyQueryIsValidatedSuccessfully("\\\"ssh login\\\"");
        verifyQueryIsValidatedSuccessfully("type:ssh");
        verifyQueryIsValidatedSuccessfully("type:(ssh OR login)");
        verifyQueryIsValidatedSuccessfully("type:\\\"ssh login\\\" ");
        verifyQueryIsValidatedSuccessfully("_exists_:type ");
        verifyQueryIsValidatedSuccessfully("NOT _exists_:type ");
        verifyQueryIsValidatedSuccessfully("/ethernet[0-9]+/");
        verifyQueryIsValidatedSuccessfully("\\\"ssh login\\\" AND source:example.org");
        verifyQueryIsValidatedSuccessfully("(\\\"ssh login\\\" AND (source:example.org OR source:another.example.org)) OR _exists_:always_find_me");
        verifyQueryIsValidatedSuccessfully("\\\"ssh login\\\" AND NOT source:example.org");
        verifyQueryIsValidatedSuccessfully("NOT example.org");
        verifyQueryIsValidatedWithValidationError("source:*.org"); //expected leading wildcard validation error with default settings
        verifyQueryIsValidatedSuccessfully("source:exam?le.org");
        verifyQueryIsValidatedSuccessfully("source:exam?le.*");
        verifyQueryIsValidatedSuccessfully("ssh logni~ ");
        verifyQueryIsValidatedSuccessfully("source:exmaple.org~");
        verifyQueryIsValidatedSuccessfully("source:exmaple.org~1 ");
        verifyQueryIsValidatedSuccessfully("\\\"foo bar\\\"~5 ");
        verifyQueryIsValidatedSuccessfully("http_response_code:[500 TO 504]");
        verifyQueryIsValidatedSuccessfully("http_response_code:{400 TO 404}");
        verifyQueryIsValidatedSuccessfully("bytes:{0 TO 64]");
        verifyQueryIsValidatedSuccessfully("http_response_code:[0 TO 64}");
        verifyQueryIsValidatedSuccessfully("http_response_code:>400");
        verifyQueryIsValidatedSuccessfully("http_response_code:<400");
        verifyQueryIsValidatedSuccessfully("http_response_code:>=400");
        verifyQueryIsValidatedSuccessfully("http_response_code:<=400");
        verifyQueryIsValidatedSuccessfully("http_response_code:(>=400 AND <500)");
        verifyQueryIsValidatedSuccessfully("timestamp:[\\\"2019-07-23 09:53:08.175\\\" TO \\\"2019-07-23 09:53:08.575\\\"]");
        verifyQueryIsValidatedSuccessfully("otherDate:[\\\"2019-07-23T09:53:08.175\\\" TO \\\"2019-07-23T09:53:08.575\\\"]");
        verifyQueryIsValidatedSuccessfully("otherDate:[\\\"2020-07-29T12:00:00.000-05:00\\\" TO \\\"2020-07-30T15:13:00.000-05:00\\\"]");
        verifyQueryIsValidatedSuccessfully("otherDate:[now-5d TO now-4d]");
        verifyQueryIsValidatedSuccessfully("resource:\\/posts\\/45326");
    }

    private void verifyQueryIsValidatedSuccessfully(final String query) {
        given()
                .spec(requestSpec)
                .when()
                .body("{\"query\": \"" + query + "\"}")
                .post("/search/validate")
                .then()
                .statusCode(200)
                .log().ifStatusCodeMatches(not(200))
                .log().ifValidationFails()
                .assertThat().body("status", equalTo("OK"));
    }

    private void verifyQueryIsValidatedWithValidationError(final String query) {
        given()
                .spec(requestSpec)
                .when()
                .body("{\"query\": \"" + query + "\"}")
                .post("/search/validate")
                .then()
                .statusCode(200)
                .log().ifStatusCodeMatches(not(200))
                .log().ifValidationFails()
                .assertThat().body("status", equalTo("ERROR"));
    }
}
