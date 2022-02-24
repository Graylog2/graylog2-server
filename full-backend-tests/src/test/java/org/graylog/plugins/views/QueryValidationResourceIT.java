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
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS)
public class QueryValidationResourceIT {

    private final RequestSpecification requestSpec;

    public QueryValidationResourceIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
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
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("WARNING"));
        validatableResponse.assertThat().body("explanations.error_message[0]", containsString("Query contains invalid operator \"not\". All AND / OR / NOT operators have to be written uppercase"));
    }

    @ContainerMatrixTest
    void testQueryParameters() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"query\":\"lorem:$ipsum$ AND\\ndolor:$sit$\"}")
                .post("/search/validate")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("status", equalTo("ERROR"));

        validatableResponse.assertThat().body("explanations.error_message[0]", containsString("Unbound required parameter used: ipsum"));
        validatableResponse.assertThat().body("explanations.begin_line[0]", equalTo(1));
        validatableResponse.assertThat().body("explanations.begin_column[0]", equalTo(6));
        validatableResponse.assertThat().body("explanations.end_line[0]", equalTo(1));
        validatableResponse.assertThat().body("explanations.end_column[0]", equalTo(13));
        validatableResponse.assertThat().body("explanations.related_property[0]", equalTo("ipsum"));

        validatableResponse.assertThat().body("explanations.error_message[1]", containsString("Unbound required parameter used: sit"));
        validatableResponse.assertThat().body("explanations.begin_line[1]", equalTo(2));
        validatableResponse.assertThat().body("explanations.begin_column[1]", equalTo(6));
        validatableResponse.assertThat().body("explanations.end_line[1]", equalTo(2));
        validatableResponse.assertThat().body("explanations.end_column[1]", equalTo(11));
        validatableResponse.assertThat().body("explanations.related_property[1]", equalTo("sit"));

    }
}
