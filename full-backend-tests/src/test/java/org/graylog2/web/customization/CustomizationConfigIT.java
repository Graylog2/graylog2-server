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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS)
public class CustomizationConfigIT {
    private final GraylogApis apis;

    public CustomizationConfigIT(GraylogApis graylogApis) {
        this.apis = graylogApis;
    }

    @ContainerMatrixTest
    void worksWithoutCustomizationConfig() {
        assertThat(getFromConfigJs("branding")).isEqualTo(null);
    }

    @ContainerMatrixTest
    void invalidCustomizationConfigDoesNotBreakEndpoint() {
        importFixture("invalid-customization-config.json");

        assertThat(getFromConfigJs("branding")).isEqualTo(null);
    }

    @ContainerMatrixTest
    void returnsCustomizationConfig() {
        importFixture("valid-customization-config.json");

        assertThat(getFromConfigJs("branding.product_name")).isEqualTo("AwesomeLog");
    }

    private String getFromConfigJs(String attribute) {
        final var response = given()
                .baseUri(apis.backend().uri())
                .port(apis.backend().apiPort())
                .get("/config.js")
                .then()
                .assertThat()
                .statusCode(200)
                .extract().body().asString();

        try (final var jsContext = Context.newBuilder()
                .allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.NONE)
                .build()) {
            final var value = jsContext.eval("js", """
                        const window = {};
                        %1$s
                        window.appConfig.%2$s;
                    """.formatted(response, attribute));
            return value.asString();
        }
    }

    void importFixture(String name) {
        apis.backend().dropCollection("cluster_config");
        apis.backend().importMongoDBFixture(name, CustomizationConfigIT.class);
    }
}
