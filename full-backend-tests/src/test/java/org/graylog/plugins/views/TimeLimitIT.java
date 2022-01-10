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
package org.graylog.plugins.views;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.AfterEach;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class TimeLimitIT {
    private final RequestSpecification requestSpec;

    public TimeLimitIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @AfterEach
    public void resetConfig() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/cluster-search-config-reset.json"))
                .put("/system/cluster_config/org.graylog2.indexer.searches.SearchesClusterConfig")
                .then()
                .statusCode(202);
    }

    @ContainerMatrixTest
    void testQueryTimeRangeLimit() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/cluster-search-config.json"))
                .put("/system/cluster_config/org.graylog2.indexer.searches.SearchesClusterConfig")
                .then()
                .statusCode(202)
                .body("query_time_range_limit", equalTo("PT2M"));

        final String body = given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat().body("execution.completed_exceptionally", equalTo(true))
                .extract()
                .body().asString();
        assertThat(body).contains("Search out of allowed time range limit");
    }
}
