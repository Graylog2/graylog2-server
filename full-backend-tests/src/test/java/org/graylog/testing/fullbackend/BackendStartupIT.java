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
package org.graylog.testing.fullbackend;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.SearchUtils;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration
class BackendStartupIT {

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public BackendStartupIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void canReachApi() {
        given()
                .config(sut.withGraylogBackendFailureConfig())
                .spec(requestSpec)
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    @ContainerMatrixTest
    void loadsDefaultPlugins() {
        List<Object> pluginNames =
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/system/plugins")
                        .then()
                        .statusCode(200)
                        .extract().jsonPath()
                        .getList("plugins.name");

        assertThat(pluginNames).containsAnyOf(
                "Elasticsearch 6 Support",
                "Elasticsearch 7 Support");
    }

    @ContainerMatrixTest
    void importsElasticsearchFixtures() {
        sut.importElasticsearchFixture("one-message.json", getClass());
        assertThat(SearchUtils.waitForMessage(requestSpec, "hello from es fixture")).isTrue();
    }
}
