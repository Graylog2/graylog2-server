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
import org.graylog.storage.elasticsearch7.ElasticsearchInstanceES7Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES7Factory.class, extraPorts = {SearchSyncIT.GELF_HTTP_PORT})
public class SearchSyncIT {

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SearchSyncIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/search/sync")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("Search body is mandatory"));
    }

    @Test
    void testMinimalisticRequest() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"Hello there\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        // mainly because of the waiting logic
        final List<String> strings = SearchUtils.searchForAllMessages(requestSpec);
        assertThat(strings.size()).isEqualTo(1);

        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("execution.completed_exceptionally", equalTo(false));
        validatableResponse.assertThat().body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("Hello there"));
    }
}
