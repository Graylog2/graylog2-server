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
package org.graylog.shared.system.stats;

import io.restassured.specification.RequestSpecification;
import org.graylog.storage.elasticsearch6.ElasticsearchInstanceES6Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

// This test doesn't seem to work within github runners
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKSPACE", matches = ".+")
@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES6Factory.class)
public class SystemStatsIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SystemStatsIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void filesystemStats() {
        final Map<Object, Object> filesystems = given()
                .spec(requestSpec)
                .when()
                .get("/system/stats")
                .then()
                .statusCode(200)
                .extract().jsonPath().getMap("fs.filesystems");

        assertThat(filesystems).isNotEmpty();
        assertThat(filesystems.get("/usr/share/graylog/data/journal")).satisfies(entry ->
                assertThat(((HashMap) entry).get("mount")).isEqualTo("/"));
    }
}
