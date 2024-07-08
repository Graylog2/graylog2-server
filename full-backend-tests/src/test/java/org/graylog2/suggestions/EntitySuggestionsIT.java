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

import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.OS2_LATEST)
public class EntitySuggestionsIT {
    private final GraylogApis api;

    public EntitySuggestionsIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void returnsTitlesForDashboards() {
        final var randomIdentifier = RandomStringUtils.randomAlphanumeric(8);
        final var dashboard1 = api.dashboards().createDashboard("First " + randomIdentifier);
        final var dashboard2 = api.dashboards().createDashboard("Second " + randomIdentifier);
        final var dashboard3 = api.dashboards().createDashboard("Third " + randomIdentifier);
        retrieveSuggestions(1, 100, randomIdentifier)
                .body("pagination.total", equalTo(3))
                .body("pagination.count", equalTo(3))
                .body("suggestions[0].value", equalTo("First " + randomIdentifier))
                .body("suggestions[0].id", equalTo(dashboard1))
                .body("suggestions[1].value", equalTo("Second " + randomIdentifier))
                .body("suggestions[1].id", equalTo(dashboard2))
                .body("suggestions[2].value", equalTo("Third " + randomIdentifier))
                .body("suggestions[2].id", equalTo(dashboard3));
        retrieveSuggestions(1, 1, randomIdentifier)
                .body("pagination.total", equalTo(3))
                .body("pagination.count", equalTo(1))
                .body("suggestions[0].value", equalTo("First " + randomIdentifier))
                .body("suggestions[0].id", equalTo(dashboard1));
        retrieveSuggestions(2, 1, randomIdentifier)
                .body("pagination.total", equalTo(3))
                .body("pagination.count", equalTo(1))
                .body("suggestions[0].value", equalTo("Second " + randomIdentifier))
                .body("suggestions[0].id", equalTo(dashboard2));
        retrieveSuggestions(3, 1, randomIdentifier)
                .body("pagination.total", equalTo(3))
                .body("pagination.count", equalTo(1))
                .body("suggestions[0].value", equalTo("Third " + randomIdentifier))
                .body("suggestions[0].id", equalTo(dashboard3));
        retrieveSuggestions(4, 1, randomIdentifier)
                .body("pagination.total", equalTo(3))
                .body("pagination.count", equalTo(0))
                .body("suggestions[0]", nullValue());
    }

    private ValidatableResponse retrieveSuggestions(int page, int perPage, String query) {
        return given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/entity_suggestions?page=" + page + "&per_page=" + perPage + "&collection=dashboards&column=title&query=" + query)
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200);
    }
}
