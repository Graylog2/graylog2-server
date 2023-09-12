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
import org.graylog.testing.completebackend.MailServerInstance;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.SearchUtils;
import org.hamcrest.Matchers;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.VM, withMailServerEnabled = true)
class BackendStartupIT {
    private final GraylogApis api;

    public BackendStartupIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void canReachApi() {
        given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    @ContainerMatrixTest
    void loadsDefaultPlugins() {
        List<Object> pluginNames =
                given()
                        .spec(api.requestSpecification())
                        .when()
                        .get("/system/plugins")
                        .then()
                        .statusCode(200)
                        .extract().jsonPath()
                        .getList("plugins.name");

        assertThat(pluginNames).containsAnyOf(
                "Elasticsearch 6 Support",
                "Elasticsearch 7 Support",
                "Threat Intelligence Plugin"
        );
    }

    @ContainerMatrixTest
    void importsElasticsearchFixtures() {
        this.api.backend().importElasticsearchFixture("one-message.json", getClass());
        assertThat(SearchUtils.waitForMessage(this.api.requestSpecificationSupplier(), "hello from es fixture")).isTrue();
    }

    @ContainerMatrixTest
    void startsMailServer() {
        final MailServerInstance mailServer = this.api.backend().getEmailServerInstance().orElseThrow(() -> new IllegalStateException("Mail server should be accessible"));
        given()
                .get(mailServer.getEndpointURI() + "/api/v2/messages")
                .then()
                .assertThat().body("count", Matchers.equalTo(0));
    }
}
