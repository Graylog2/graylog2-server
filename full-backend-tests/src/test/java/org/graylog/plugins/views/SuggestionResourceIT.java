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
import org.graylog.testing.containermatrix.ContainerVersions;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS, extraPorts = SuggestionResourceIT.GELF_HTTP_PORT, esVersions = {ContainerVersions.ES6, ContainerVersions.ES7})
public class SuggestionResourceIT {

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SuggestionResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void testMinimalRequest() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#testMinimalisticRequest\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        // mainly because of the waiting logic
        final boolean messagePresent = SearchUtils.searchForMessage(requestSpec, "SuggestionResourceIT#testMinimalisticRequest");
        assertThat(messagePresent).isTrue();

        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", equalTo(1));
    }

    @ContainerMatrixTest
    void testInvalidField() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#testInvalidField\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        // mainly because of the waiting logic
        final boolean messagePresent = SearchUtils.searchForMessage(requestSpec, "SuggestionResourceIT#testInvalidField");
        assertThat(messagePresent).isTrue();

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
}
