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
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class SearchMetadataIT {
    private final RequestSpecification requestSpec;
    private final GraylogBackend graylogBackend;

    public SearchMetadataIT(GraylogBackend graylogBackend, RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
        this.graylogBackend = graylogBackend;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.graylogBackend.importMongoDBFixture("mongodb-stored-searches-for-metadata-endpoint.json", SearchMetadataIT.class);
    }

    @ContainerMatrixTest
    void testEmptyRequest() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/search/metadata")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("Search body is mandatory"));
    }

    @ContainerMatrixTest
    void testMinimalRequestWithoutParameter() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/metadata")
                .then()
                .statusCode(200);

        response.assertThat().body("query_metadata*.value.used_parameters_names[0]", empty());
        response.assertThat().body("declared_parameters", anEmptyMap());
    }

    @ContainerMatrixTest
    void testMinimalRequestWithSingleParameter() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request-with-undeclared-parameter.json"))
                .post("/views/search/metadata")
                .then()
                .statusCode(200);

        response.assertThat().body("query_metadata.f1446410-a082-4871-b3bf-d69aa42d0c96.used_parameters_names", contains("action"));
        response.assertThat().body("declared_parameters", anEmptyMap());
    }

    @ContainerMatrixTest
    void testRetrievingMetadataForStoredSearchWithoutParameter() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .when()
                .get("/views/search/metadata/61977428c1f17d26b45c8a0b")
                .then()
                .statusCode(200);

        response.assertThat().body("query_metadata.f1446410-a082-4871-b3bf-d69aa42d0c96.used_parameters_names", empty());
        response.assertThat().body("declared_parameters", anEmptyMap());
    }

    @ContainerMatrixTest
    void testRetrievingMetadataForStoredSearchWithParameter() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .when()
                .get("/views/search/metadata/61977043c1f17d26b45c8a0a")
                .then()
                .statusCode(200);

        response.assertThat().body("query_metadata.f1446410-a082-4871-b3bf-d69aa42d0c96.used_parameters_names", contains("action"));
        response.assertThat().body("declared_parameters", anEmptyMap());
    }

    private InputStream fixture(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
