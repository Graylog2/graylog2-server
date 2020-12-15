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
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

abstract class MessagesResourceIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public MessagesResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void canDownloadCsv() {
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
