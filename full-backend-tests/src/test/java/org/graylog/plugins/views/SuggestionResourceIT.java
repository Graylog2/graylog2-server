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
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.VM;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = VM)
public class SuggestionResourceIT {

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SuggestionResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void init() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#1\", \"host\":\"example.org\", \"facility\":\"junit\"}",
                requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#2\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#3\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#1");
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#2");
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#3");
    }

    @ContainerMatrixTest
    void testMinimalRequest() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(2));
    }

    @ContainerMatrixTest
    void testInvalidField() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"message\", \"input\":\"foo\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        // error types and messages are different for each ES version, so let's just check that there is an error in the response
        validatableResponse.assertThat().body("error", notNullValue());
    }

    @ContainerMatrixTest
    void testSizeOtherDocsCount() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"\", \"size\":1}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(2));
        validatableResponse.assertThat().body("sum_other_docs_count", greaterThanOrEqualTo(1));
    }

    @ContainerMatrixTest
    void testTypoCorrection() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"tets\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(1));
    }

}
