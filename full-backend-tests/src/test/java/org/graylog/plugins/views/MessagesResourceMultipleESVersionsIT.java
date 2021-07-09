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
import org.graylog.storage.ElasticSearchInstanceFactoryByVersion;
import org.graylog.testing.ESVersionTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.MultipleESVersionsTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@MultipleESVersionsTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticSearchInstanceFactoryByVersion.class, esVersions = { "6.8.4", "7.10.2" })
//@EnabledIf("org.graylog.testing.MultipleESVersionsTestEngine#testWithMultipleESVersions")
@Disabled
class MessagesResourceMultipleESVersionsIT implements ESVersionTest {
    private GraylogBackend backend;
    private RequestSpecification requestSpec;

    public void setEsVersion(GraylogBackend backend, RequestSpecification specification) {
        this.backend = backend;
        this.requestSpec = specification;
    }

    @Test
    boolean canDownloadCsv() {
        backend.importElasticsearchFixture("messages-for-export.json", MessagesResourceMultipleESVersionsIT.class);

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

        return true;
    }
}
