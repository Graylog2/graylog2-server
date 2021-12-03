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

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class SearchSyncIT {

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SearchSyncIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void importMongoFixtures() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"search-sync-test\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        // mainly because of the waiting logic
        final boolean isMessagePresent = SearchUtils.waitForMessage(requestSpec, "search-sync-test");
        assertThat(isMessagePresent).isTrue();
    }

    @ContainerMatrixTest
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/search/sync")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("Search body is mandatory"));
    }

    @ContainerMatrixTest
    void testMinimalisticRequest() {
        given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    @ContainerMatrixTest
    void testMinimalisticRequestv2() {
        given()
                .spec(requestSpec)
                .accept("application/vnd.graylog.search.v2+json")
                .contentType("application/vnd.graylog.search.v2+json")
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    @ContainerMatrixTest
    void testRequestWithStreamsv2() {
        given()
                .spec(requestSpec)
                .accept("application/vnd.graylog.search.v2+json")
                .contentType("application/vnd.graylog.search.v2+json")
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request-with-streams.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    private InputStream fixture(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
