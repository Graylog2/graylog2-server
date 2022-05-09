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
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.graylog.testing.graylognode.NodeContainerConfig.GELF_HTTP_PORT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS, mongoVersions = MongodbServer.MONGO4, searchVersions = SearchServer.OS1)
public class QueryValidationResourceIT {

    private final RequestSpecification requestSpec;
    private final GraylogBackend sut;

    public QueryValidationResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void importMessage() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"query-validation-test\", \"host\":\"example.org\", \"level\":3}",
                requestSpec);

        // mainly because of the waiting logic
        final boolean isMessagePresent = SearchUtils.waitForMessage(requestSpec, "query-validation-test");
        assertThat(isMessagePresent).isTrue();

        SearchUtils.waitForFieldTypeDefinition(requestSpec, "level");
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
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"/ethernet[0-9]+/\"}")
                .post("/search/validate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("OK"));
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
    void testQuotedDefaultField() {
        given()
                .spec(requestSpec)
                .when()
                .body("{\"query\": \"\\\"A or B\\\"\"}")
                .post("/search/validate")
                .then()
                .statusCode(200)
                .log().ifStatusCodeMatches(not(200))
                .log().ifValidationFails()
                // if the validation correctly recognizes the quoted text, it should not warn about lowercase or
                .assertThat().body("status", equalTo("OK"));
    }
}
