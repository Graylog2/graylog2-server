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
package org.graylog.testing.utils;

import io.restassured.specification.RequestSpecification;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class IndexSetUtils {
    private IndexSetUtils() {}

    public static String defaultIndexSetId(Supplier<RequestSpecification> spec) {
        return given()
                .spec(spec.get())
                .when()
                .get("/system/indices/index_sets")
                .then()
                .statusCode(200)
                .assertThat()
                .extract().body().jsonPath().getString("index_sets.find { it.default == true }.id");
    }

    public static String createIndexSet(Supplier<RequestSpecification> spec, IndexSetSummary indexSetSummary) {
        return given()
                .spec(spec.get())
                .log().ifValidationFails()
                .when()
                .body(indexSetSummary)
                .post("/system/indices/index_sets")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
    }

    public static String createIndexSet(Supplier<RequestSpecification> spec, String title, String description, String prefix) {
        var indexSetSummary = IndexSetSummary.create(null,
                title,
                description,
                false,
                true,
                false,
                prefix,
                4,
                0,
                "org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy",
                TimeBasedRotationStrategyConfig.builder()
                        .rotationPeriod(Period.days(1))
                        .rotateEmptyIndexSet(false)
                        .build(),
                "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy",
                DeletionRetentionStrategyConfig.create(20),
                ZonedDateTime.now(ZoneId.of("UTC")),
                "standard",
                1,
                false,
                Duration.standardSeconds(5L),
                null,
                null
        );

        return createIndexSet(spec, indexSetSummary);
    }
}
