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
package org.graylog.testing.completebackend.apis;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class Dashboards implements GraylogRestApi {
    private final GraylogApis api;

    public Dashboards(GraylogApis api) {
        this.api = api;
    }

    public String createDashboard(String title) {
        final var searchId = given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of())
                .post("/views/search")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(createDashboardRequest(title, searchId))
                .post("/views")
                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
    }

    private Map<String, Object> createDashboardRequest(String title, String searchId) {
        return Map.of(
                "title", title,
                "search_id", searchId,
                "state", Map.of(),
                "type", "DASHBOARD"
        );
    }
}
