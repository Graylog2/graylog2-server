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
package org.graylog.testing.completebackend.apis;

import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class Rules {
    private static final String URL_PREFIX = "/system/pipelines/rule";
    private final GraylogApis api;

    public Rules(GraylogApis api) {
        this.api = api;
    }

    public ValidatableResponse parse(String source) {
        return parse(source, HttpStatus.SC_OK);
    }

    public ValidatableResponse parse(String source, int expectedStatus) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of("source", source))
                .post(URL_PREFIX + "/parse")
                .then()
                .log().ifError()
                .statusCode(expectedStatus);
    }

    public ValidatableResponse simulate(String message, String ruleSource) {
        return simulate(message, ruleSource, HttpStatus.SC_OK);
    }

    public ValidatableResponse simulate(String message, String ruleSource, int expectedStatus) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of("message", message, "rule_source", Map.of("source", ruleSource)))
                .post(URL_PREFIX + "/simulate")
                .then()
                .log().ifError()
                .statusCode(expectedStatus);
    }
}
