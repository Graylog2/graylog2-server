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

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class MessagesResourceIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public MessagesResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void importMessages() {
        sut.importElasticsearchFixture("messages-for-export.json", MessagesResourceIT.class);
    }

    @ContainerMatrixTest
    void testInvalidQuery() {
        String allMessagesTimeRange = "{\"query_string\":\"foo:\", \"timerange\": {\"type\": \"absolute\", \"from\": \"2015-01-01T00:00:00\", \"to\": \"2015-01-01T23:59:59\"}}";
        given()
                .spec(requestSpec)
                .accept("text/csv")
                .body(allMessagesTimeRange)
                .post("/views/search/messages")
                .then()
                .statusCode(400).contentType("application/json")
                .assertThat().body("message", Matchers.startsWith("Request validation failed"));
    }

    @ContainerMatrixTest
    void testInvalidQueryResponse() {
        sut.importElasticsearchFixture("messages-for-export.json", MessagesResourceIT.class);

        String allMessagesTimeRange = "{\"timerange\": {\"type\": \"absolute\", \"from\": \"2015-01-01T00:00:00\", \"to\": \"2015-01-01T23:59:59\"}}";

        Response r = given()
                .spec(requestSpec)
                .accept("text/csv")
                .body(allMessagesTimeRange)
                .expect().response().statusCode(200).contentType("text/csv")
                .when()
                .post("/views/search/messages");

        String[] resultLines = r.asString().split("\n");

        assertThat(resultLines)
                .startsWith("\"timestamp\",\"source\",\"message\"")
                .as("should contain header");

        assertThat(Arrays.copyOfRange(resultLines, 1, 5)).containsExactlyInAnyOrder(
                "\"2015-01-01T04:00:00.000Z\",\"source-2\",\"Ho\"",
                "\"2015-01-01T03:00:00.000Z\",\"source-1\",\"Hi\"",
                "\"2015-01-01T02:00:00.000Z\",\"source-2\",\"He\"",
                "\"2015-01-01T01:00:00.000Z\",\"source-1\",\"Ha\""
        );
    }
}
