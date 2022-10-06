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
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class ViewsResourceIT {
    private final RequestSpecification requestSpec;

    public ViewsResourceIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("View is mandatory"));
    }

    @ContainerMatrixTest
    void testCreateViewRequestWithoutPersistedSearch() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/views-request.json"))
                .post("/views")
                .then()
                .statusCode(400)
                .assertThat().body("message", equalTo("Search 6141d457d3a6b9d73c8ac55a not available"));
    }

    @ContainerMatrixTest
    void testCreateSearchPersistView() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/save-search-request.json"))
                .post("/views/search")
                .then()
                .statusCode(201);

        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/views-request.json"))
                .post("/views")
                .then()
                .statusCode(200);
    }

    @ContainerMatrixTest
    void testInvalidSearchType() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/save-search-request-invalid.json"))
                .post("/views/search")
                .then()
                .statusCode(201);

        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/views-request-invalid-search-type.json"))
                .post("/views")
                .then()
                .statusCode(400)
                .assertThat()
                .body("message", equalTo("Search types do not correspond to view/search types, missing searches [967d2217-fd99-48a6-b829-5acdab906808]; search types: [967d2217-fd99-48a6-b829-5acdab906807]; state types: [967d2217-fd99-48a6-b829-5acdab906808]"));
    }
}
