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
package org.graylog2.web.customization;

import io.restassured.response.ValidatableResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.DATANODE_DEV)
public class CustomizationConfigIT {
    private final GraylogApis apis;

    public CustomizationConfigIT(GraylogApis graylogApis) {
        this.apis = graylogApis;
    }

    @ContainerMatrixTest
    void worksWithoutCustomizationConfig() {
        getConfigJs()
                .body("branding", nullValue());
    }

    @ContainerMatrixTest
    void invalidCustomizationConfigDoesNotBreakEndpoint() {
        importFixture("invalid-customization-config.json");

        getConfigJs()
                .body("branding", nullValue());
    }

    @ContainerMatrixTest
    void returnsCustomizationConfig() {
        importFixture("valid-customization-config.json");

        getConfigJs()
                .body("branding.product_name", equalTo("AwesomeLog"));
    }

    private ValidatableResponse getConfigJs() {
        return given()
                .baseUri(apis.backend().uri())
                .port(apis.backend().apiPort())
                .get("/config.js")
                .then()
                .assertThat()
                .statusCode(200);
    }

    void importFixture(String name) {
        apis.backend().importMongoDBFixture(name, CustomizationConfigIT.class);
    }
}
