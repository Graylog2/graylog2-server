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

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, additionalConfigurationParameters = {
        @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_LEADER_ELECTION_MODE", value = "automatic")
})
class AutomaticLeaderElectionStartupIT {
    private final GraylogApis api;

    public AutomaticLeaderElectionStartupIT(GraylogApis api) {
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
}
