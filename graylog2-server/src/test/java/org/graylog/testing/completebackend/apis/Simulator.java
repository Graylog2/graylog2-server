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

public class Simulator {
    private static final String URL_PREFIX = "/system/pipelines/simulate";
    private final GraylogApis api;

    public Simulator(GraylogApis api) {
        this.api = api;
    }

    public ValidatableResponse simulate(String streamId, Map<String, Object> message) {
        return simulate(streamId, message, HttpStatus.SC_OK);
    }

    public ValidatableResponse simulate(String streamId, Map<String, Object> message, int expectedStatus) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of("stream_id", streamId, "message", message))
                .post(URL_PREFIX)
                .then()
                .log().ifError()
                .statusCode(expectedStatus);
    }
}
