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

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.VM;

@ContainerMatrixTestsConfiguration(serverLifecycle = VM, mongoVersions = {MongodbServer.MONGO5})
class MongoDBFixturesWithVMLifecycleIT {
    private final GraylogApis api;

    public MongoDBFixturesWithVMLifecycleIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.api.backend().importMongoDBFixture("access-token.json", MongoDBFixturesWithVMLifecycleIT.class);
    }

    @ContainerMatrixTest
    void oneTokenPresentWithTestMethodA() {
        assertTokenPresent();
    }

    @ContainerMatrixTest
    void oneTokenPresentWithTestMethodB() {
        assertTokenPresent();
    }

    private void assertTokenPresent() {
        List<?> tokens = given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .get("users/local:admin/tokens")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("tokens");

        assertThat(tokens).hasSize(1);
    }

}
