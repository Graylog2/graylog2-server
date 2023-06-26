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

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.shared.rest.resources.csp.CSP;
import org.graylog2.shared.rest.resources.csp.CSPResponseFilter;
import org.hamcrest.Matchers;

import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, withMailServerEnabled = true)
public class FiltersIT {
    private final GraylogApis api;
    private static final Pattern defaultCSPPattern = Pattern.compile(Pattern.quote(CSP.CSP_DEFAULT)
            .replaceAll("\\{nonce}", "\\\\E[a-zA-Z0-9-]+\\\\Q"));

    public FiltersIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void cspDocumentationBrowser() {
        String expected = CSP.CSP_SWAGGER;
        given()
                .spec(api.requestSpecification())
                .when()
                .get("/api-browser")
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo(expected));
    }

    @ContainerMatrixTest
    void cspWebInterfaceAssets() {
        given()
                .spec(api.requestSpecification())
                .basePath("/")
                .when()
                .get()
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.matchesPattern(defaultCSPPattern));
    }

    @ContainerMatrixTest
    void cspWebAppNotFound() {
        given()
                .spec(api.requestSpecification())
                .basePath("/")
                .when()
                .get("streams")
                .then()
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.matchesPattern(defaultCSPPattern));
    }
}
