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
import org.graylog2.shared.rest.resources.csp.CSPResources;
import org.graylog2.shared.rest.resources.csp.CSPResponseFilter;
import org.hamcrest.Matchers;

import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, withMailServerEnabled = true)
public class FiltersIT {
    private static final String DEFAULT_CONNECT_SRC = "connect-src 'self' https://graylog.org/post/tag/ https://telemetry.graylog.cloud;";
    private final GraylogApis api;
    private final CSPResources cspResources;
    private final Pattern defaultCSPPattern;

    public FiltersIT(GraylogApis api) {
        this.api = api;
        this.cspResources = new CSPResources();
        this.defaultCSPPattern = Pattern.compile(Pattern.quote(DEFAULT_CONNECT_SRC + cspResources.cspString(CSP.DEFAULT))
                .replaceAll("\\{nonce}", "\\\\E[a-zA-Z0-9-]+\\\\Q"));
    }

    @ContainerMatrixTest
    void cspDocumentationBrowser() {
        String expected = cspResources.cspString(CSP.SWAGGER);
        given()
                .spec(api.requestSpecification())
                .when()
                .get("/api-browser")
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.containsString(expected));
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
