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
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.shared.rest.CSPResponseFilter;
import org.graylog2.shared.rest.resources.annotations.CSP;
import org.hamcrest.Matchers;

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration
public class FiltersIT {
    private final RequestSpecification requestSpec;

    public FiltersIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void cspDocumentationBrowser() {
        given()
                .spec(requestSpec)
                .when()
                .get("/api-browser")
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo(CSP.CSP_SWAGGER));
    }

    @ContainerMatrixTest
    void cspWebInterfaceAssets() {
        given()
                .spec(requestSpec)
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo(CSP.CSP_DEFAULT));
    }

    @ContainerMatrixTest
    void cspWebAppNotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/streams")
                .then()
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo(CSP.CSP_DEFAULT));
    }
}
