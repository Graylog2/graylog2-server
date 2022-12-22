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

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class StartPageIT {
    private final RequestSpecification requestSpec;
    private final GraylogApis api;

    private static final String username = "john.doe2";
    private static final String password = "asdfgh";

    public StartPageIT(RequestSpecification requestSpec, GraylogApis apis) {
        this.requestSpec = requestSpec;
        this.api = apis;
    }

    @BeforeAll
    public void init() {
        final JsonPath user = api.users().createUser(new Users.User(
                this.username,
                this.password,
                "John",
                "Doe",
                "john.doe2@example.com",
                false,
                30_000,
                "Europe/Vienna",
                List.of("Admin"),
                Collections.emptyList()
        ));
    }

    private ValidatableResponse post(final String url, final String resource, final int expectedResult) {
        return given()
                .spec(requestSpec)
                .auth().basic(username, password)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream(resource))
                .post(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    private ValidatableResponse get(final String url, final String username, final String password, final int expectedResult) {
        return given()
                .spec(requestSpec)
                .auth().basic(username, password)
                .when()
                .get(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    private ValidatableResponse get(final String url, final int expectedResult) {
        return get(url, this.username, this.password, expectedResult);
    }

    @ContainerMatrixTest
    void testCreateLastOpenedItem() {
        post("/views/search", "org/graylog/plugins/views/save-search-request.json", 201);
        post("/views", "org/graylog/plugins/views/startpage-views-request.json", 200);

        var validatableResponse = get("/views", 200);
        var id = validatableResponse.extract().jsonPath().get("views[0]._id");

        get("/views/" + id, 200);
        validatableResponse = get("/startpage/lastOpened", 200);
        validatableResponse.assertThat().body("lastOpened[0].id", equalTo(id));
    }


    @ContainerMatrixTest
    void testCreateRecentActivity() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        var stream1Id = api.streams().createStream("Stream #1", defaultIndexSetId, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "stream1", "target_stream", false));

        var validatableResponse = get("/startpage/recentActivity", "admin", "admin", 200).log().body();
        validatableResponse.assertThat().body("recentActivity[0].item_id", equalTo(stream1Id));
    }
}
