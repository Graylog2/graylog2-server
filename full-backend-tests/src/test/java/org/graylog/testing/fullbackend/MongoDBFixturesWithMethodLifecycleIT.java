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
import org.graylog.storage.elasticsearch6.ElasticsearchInstanceES6Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;


@ApiIntegrationTest(serverLifecycle = Lifecycle.METHOD, elasticsearchFactory = ElasticsearchInstanceES6Factory.class,
        mongoDBFixtures = "access-token.json")
class MongoDBFixturesWithMethodLifecycleIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public MongoDBFixturesWithMethodLifecycleIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void oneTokenPresentWithTestMethodA() {
        assertTokenPresent();
    }

    @Test
    void oneTokenPresentWithTestMethodB() {
        assertTokenPresent();
    }

    private void assertTokenPresent() {
        List<?> tokens = given().spec(requestSpec)
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
