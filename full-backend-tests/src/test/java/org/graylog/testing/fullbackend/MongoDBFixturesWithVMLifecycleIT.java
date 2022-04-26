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
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.VM;

@ContainerMatrixTestsConfiguration(serverLifecycle = VM, searchVersions = {SearchServer.ES6}, mongoVersions = {MongodbServer.MONGO4})
class MongoDBFixturesWithVMLifecycleIT {
    private final GraylogBackend graylogBackend;
    private final RequestSpecification requestSpec;

    public MongoDBFixturesWithVMLifecycleIT(GraylogBackend graylogBackend, RequestSpecification requestSpec) {
        this.graylogBackend = graylogBackend;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.graylogBackend.importMongoDBFixture("access-token.json", MongoDBFixturesWithVMLifecycleIT.class);
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
                .config(graylogBackend.withGraylogBackendFailureConfig())
                .spec(requestSpec)
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
