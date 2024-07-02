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
package org.graylog2.suggestions;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration
public class EntitySuggestionsIT {
    private final GraylogApis api;

    public EntitySuggestionsIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void returnsTitlesForDashboards() {
        final var dashboard1 = api.dashboards().createDashboard("Test");
        final var dashboard2 = api.dashboards().createDashboard("Test 2");
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/entity_suggestions?page=1&per_page=100&collection=dashboards&column=title&query=Test")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("pagination.total", equalTo(2))
                .body("suggestions[0].value", equalTo("Test"))
                .body("suggestions[0].id", equalTo(dashboard1))
                .body("suggestions[1].value", equalTo("Test 2"))
                .body("suggestions[1].id", equalTo(dashboard2));
    }
}
